package app.attic.player.data

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Runs the hand-written migrations on real SQLite: a database built from one of Room's exported schemas,
 * with data in it, must end up with exactly the tables Room exported for the version it migrates to,
 * data intact. Version 4 adds `song_styles`, version 5 `song_loudness`, version 6 `song_transitions`
 * (smart transitions' version, carried from that branch: its `6.json` is the schema the owner's phone
 * has), version 7 the equaliser's `eq_settings` and `eq_presets`, version 8 mood mixes' `song_moods`
 * and the embedding's length in `song_analysis`.
 */
class DatabaseMigrationTest {
    private val schemas = File("schemas/app.attic.player.data.AtticDatabase")
    private val db: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    @After
    fun close() = db.close()

    private fun schema(version: Int): JSONObject = JSONObject(File(schemas, "$version.json").readText()).getJSONObject("database")

    /** Table name to its CREATE statement, as Room would run it. */
    private fun createStatements(schema: JSONObject): Map<String, String> {
        val entities = schema.getJSONArray("entities")
        return (0 until entities.length()).map(entities::getJSONObject).associate { entity ->
            val name = entity.getString("tableName")
            name to entity.getString("createSql").replace("\${TABLE_NAME}", name)
        }
    }

    /** The same statements as SQLite keeps them once run, without "IF NOT EXISTS". */
    private fun storedStatements(version: Int): Map<String, String> =
        createStatements(schema(version)).mapValues { (_, sql) -> sql.removePrefix("CREATE TABLE IF NOT EXISTS ").let { "CREATE TABLE $it" } }

    private fun create(schema: JSONObject) {
        db.createStatement().use { statement ->
            createStatements(schema).values.forEach(statement::execute)
            val entities = schema.getJSONArray("entities")
            for (e in 0 until entities.length()) {
                val entity = entities.getJSONObject(e)
                val indices = entity.optJSONArray("indices") ?: continue
                for (i in 0 until indices.length()) {
                    statement.execute(indices.getJSONObject(i).getString("createSql").replace("\${TABLE_NAME}", entity.getString("tableName")))
                }
            }
        }
    }

    private fun run(statements: List<String>) = db.createStatement().use { s -> statements.forEach(s::execute) }

    private fun tables(): Map<String, String> = db.createStatement().use { statement ->
        statement.executeQuery("SELECT name, sql FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'").use { rows ->
            buildMap { while (rows.next()) put(rows.getString(1), rows.getString(2)) }
        }
    }

    /** Column name to (type affinity, not null), as SQLite reports the table. */
    private fun columns(table: String): Map<String, Pair<String, Boolean>> = db.createStatement().use { s ->
        s.executeQuery("PRAGMA table_info(`$table`)").use { rows ->
            buildMap { while (rows.next()) put(rows.getString("name"), rows.getString("type") to (rows.getInt("notnull") == 1)) }
        }
    }

    /** Every table's columns match Room's exported [version], the way Room checks them on the phone. */
    private fun assertColumnsMatch(version: Int) {
        val entities = schema(version).getJSONArray("entities")
        for (i in 0 until entities.length()) {
            val entity = entities.getJSONObject(i)
            val table = entity.getString("tableName")
            val fields = entity.getJSONArray("fields")
            val wanted = (0 until fields.length()).map { fields.getJSONObject(it) }
                .associate { it.getString("columnName") to (it.getString("affinity") to it.optBoolean("notNull")) }
            assertEquals("columns of $table", wanted, columns(table))
        }
    }

    private val embedding = byteArrayOf(1, 2, 3, 4)

    private fun insertVersion3Rows() {
        db.createStatement().use { it.execute("INSERT INTO liked (trackKey, likedAt) VALUES ('Music/a.mp3', 1)") }
        db.prepareStatement("INSERT INTO song_analysis (trackKey, modelVersion, embedding, chroma, analyzedAt) VALUES (?, 1, ?, NULL, 5)").use {
            it.setString(1, "Music/a.mp3")
            it.setBytes(2, embedding)
            it.executeUpdate()
        }
    }

    private fun assertVersion3RowsKept() {
        db.createStatement().use { s ->
            assertTrue(s.executeQuery("SELECT 1 FROM liked WHERE trackKey = 'Music/a.mp3'").next())
            s.executeQuery("SELECT trackKey, embedding FROM song_analysis").use { rows ->
                assertTrue(rows.next())
                assertEquals("Music/a.mp3", rows.getString(1))
                assertArrayEquals(embedding, rows.getBytes(2))
            }
        }
    }

    private fun count(table: String): Int = db.createStatement().use { s ->
        s.executeQuery("SELECT COUNT(*) FROM $table").use { rows ->
            rows.next()
            rows.getInt(1)
        }
    }

    @Test
    fun theMigrationsChainFromThreeToEight() {
        assertEquals(3 to 4, DatabaseMigrations.FROM_3_TO_4.startVersion to DatabaseMigrations.FROM_3_TO_4.endVersion)
        assertEquals(4 to 5, DatabaseMigrations.FROM_4_TO_5.startVersion to DatabaseMigrations.FROM_4_TO_5.endVersion)
        assertEquals(5 to 6, DatabaseMigrations.FROM_5_TO_6.startVersion to DatabaseMigrations.FROM_5_TO_6.endVersion)
        assertEquals(6 to 7, DatabaseMigrations.FROM_6_TO_7.startVersion to DatabaseMigrations.FROM_6_TO_7.endVersion)
        assertEquals(7 to 8, DatabaseMigrations.FROM_7_TO_8.startVersion to DatabaseMigrations.FROM_7_TO_8.endVersion)
        // The newest exported schema is the one the last migration reaches.
        assertEquals(8, schemas.list()!!.mapNotNull { it.removeSuffix(".json").toIntOrNull() }.max())
    }

    @Test
    fun versionSixIsSmartTransitionsSchema() {
        // The owner's phone is at this version 6, from feature/smart-transitions; the equaliser builds on it.
        assertEquals("5f86fc0f6b5eb0017f518d398245b698", schema(6).getString("identityHash"))
    }

    @Test
    fun fiveToSixAddsTransitionsAndKeepsEverything() {
        create(schema(5))
        insertVersion3Rows()
        run(DatabaseMigrations.SQL_5_TO_6)

        assertEquals(storedStatements(6), tables())
        assertColumnsMatch(6)
        assertVersion3RowsKept()
        assertEquals(0, count("song_transitions"))
    }

    @Test
    fun sixToSevenAddsTheEqualiserOnSmartTransitionsSchema() {
        create(schema(6))
        insertVersion3Rows()
        db.createStatement().use {
            it.execute("INSERT INTO song_loudness (trackKey, version, integratedLufs, samplePeak, measuredAt) VALUES ('Music/a.mp3', 1, -9.5, 0.98, 3)")
            it.execute(
                "INSERT INTO song_transitions (trackKey, version, decodedMs, audioStartMs, audioEndMs, measuredAt) " +
                    "VALUES ('Music/a.mp3', 1, 240000, 120, 239000, 4)",
            )
        }
        run(DatabaseMigrations.SQL_6_TO_7)

        assertEquals(storedStatements(7), tables())
        assertColumnsMatch(7)
        assertVersion3RowsKept()
        assertEquals(1, count("song_loudness"))
        assertEquals(1, count("song_transitions"))
        assertEquals(0, count("eq_settings"))
        db.createStatement().use {
            it.execute("INSERT INTO eq_settings (scope, `key`, gains, savedAt) VALUES ('song', 'Music/a.mp3', '0,1.5,3,0,0,0,0,0,0,0', 7)")
            it.execute("INSERT INTO eq_settings (scope, `key`, gains, savedAt) VALUES ('album', 'monsoon|kavya', '6,4,0,0,-2,0,-1,-5,5,1', 8)")
            it.execute("INSERT INTO eq_presets (name, gains, createdAt) VALUES ('Car', '3,3,0,0,0,0,0,0,2,2', 9)")
            // A song and an album may share a key; the scope keeps them apart.
            it.execute("INSERT INTO eq_settings (scope, `key`, gains, savedAt) VALUES ('album', 'Music/a.mp3', '0,0,0,0,0,0,0,0,0,0', 10)")
            it.executeQuery("SELECT gains FROM eq_settings WHERE scope = 'song' AND `key` = 'Music/a.mp3'").use { row ->
                assertTrue(row.next())
                assertEquals("0,1.5,3,0,0,0,0,0,0,0", row.getString(1))
            }
            it.executeQuery("SELECT id FROM eq_presets WHERE name = 'Car'").use { row ->
                assertTrue(row.next())
                assertEquals(1L, row.getLong(1))
            }
        }
        assertEquals(3, count("eq_settings"))
    }

    /** Column name to its default, as SQLite reports it. */
    private fun defaults(table: String): Map<String, String?> = db.createStatement().use { s ->
        s.executeQuery("PRAGMA table_info(`$table`)").use { rows ->
            buildMap { while (rows.next()) put(rows.getString("name"), rows.getString("dflt_value")) }
        }
    }

    @Test
    fun sevenToEightAddsMoodsAndTheLengthAndKeepsEverything() {
        create(schema(7))
        insertVersion3Rows()
        db.createStatement().use {
            it.execute("INSERT INTO song_styles (trackKey, modelVersion, top) VALUES ('Music/a.mp3', 1, x'0100ffff')")
            it.execute("INSERT INTO eq_presets (name, gains, createdAt) VALUES ('Car', '3,3,0,0,0,0,0,0,2,2', 9)")
        }
        run(DatabaseMigrations.SQL_7_TO_8)

        // ALTER TABLE keeps the added column's SQL text apart from Room's CREATE, so song_analysis is
        // compared by its columns and defaults, as Room compares it on the phone; every other table by its SQL.
        assertEquals(storedStatements(8) - "song_analysis", tables() - "song_analysis")
        assertEquals(storedStatements(8).keys, tables().keys)
        assertColumnsMatch(8)
        assertEquals("NULL", defaults("song_analysis")["embeddingNorm"])
        assertVersion3RowsKept()
        assertEquals(1, count("song_styles"))
        assertEquals(1, count("eq_presets"))
        db.createStatement().use { s ->
            // A song analysed before has no length yet, so it gets approximate mood scores until analysis decodes it again.
            s.executeQuery("SELECT embeddingNorm FROM song_analysis WHERE trackKey = 'Music/a.mp3'").use { row ->
                assertTrue(row.next())
                row.getFloat(1)
                assertTrue(row.wasNull())
            }
            s.executeUpdate("INSERT INTO song_moods (trackKey, version, exact, scores) VALUES ('Music/a.mp3', 1, 0, x'ffff')")
        }
        assertEquals(1, count("song_moods"))
    }

    @Test
    fun versionEightOnlyAddsTheMoodsTableAndTheLength() {
        val before = createStatements(schema(7))
        val after = createStatements(schema(8))
        assertEquals(before - "song_analysis", after - "song_analysis" - "song_moods")
        assertEquals(setOf("song_moods"), after.keys - before.keys)
        assertEquals(
            before.getValue("song_analysis").replace(", PRIMARY KEY", ", `embeddingNorm` REAL DEFAULT NULL, PRIMARY KEY"),
            after.getValue("song_analysis"),
        )
        val strip = { sql: String -> sql.replace("CREATE TABLE IF NOT EXISTS", "").trim() }
        assertEquals(strip(after.getValue("song_moods")), strip(DatabaseMigrations.SQL_7_TO_8[1]))
    }

    @Test
    fun threeToEightRunsEveryMigration() {
        create(schema(3))
        insertVersion3Rows()
        listOf(DatabaseMigrations.SQL_3_TO_4, DatabaseMigrations.SQL_4_TO_5, DatabaseMigrations.SQL_5_TO_6, DatabaseMigrations.SQL_6_TO_7, DatabaseMigrations.SQL_7_TO_8)
            .forEach(::run)
        assertEquals(storedStatements(8).keys, tables().keys)
        assertColumnsMatch(8)
        assertVersion3RowsKept()
        assertEquals(0, count("song_moods"))
    }

    @Test
    fun threeToSevenRunsEveryMigration() {
        create(schema(3))
        insertVersion3Rows()
        run(DatabaseMigrations.SQL_3_TO_4)
        run(DatabaseMigrations.SQL_4_TO_5)
        run(DatabaseMigrations.SQL_5_TO_6)
        run(DatabaseMigrations.SQL_6_TO_7)

        assertEquals(storedStatements(7), tables())
        assertColumnsMatch(7)
        assertVersion3RowsKept()
    }

    @Test
    fun versionSixOnlyAddsTheTransitionsTable() {
        val before = createStatements(schema(5))
        val after = createStatements(schema(6))
        assertEquals(before, after - "song_transitions")
        assertEquals(setOf("song_transitions"), after.keys - before.keys)
    }

    @Test
    fun versionSevenOnlyAddsTheEqualiserTables() {
        val before = createStatements(schema(6))
        val after = createStatements(schema(7))
        assertEquals(before, after - "eq_settings" - "eq_presets")
        assertEquals(setOf("eq_settings", "eq_presets"), after.keys - before.keys)
    }

    @Test
    fun threeToFourAddsStylesAndKeepsAnalysis() {
        create(schema(3))
        insertVersion3Rows()
        run(DatabaseMigrations.SQL_3_TO_4)

        assertEquals(storedStatements(4), tables())
        assertColumnsMatch(4)
        assertVersion3RowsKept()
        db.createStatement().use { it.executeUpdate("INSERT INTO song_styles (trackKey, modelVersion, top) VALUES ('Music/a.mp3', 1, x'0100ffff')") }
        assertEquals(1, count("song_styles"))
    }

    @Test
    fun fourToFiveAddsLoudnessAndKeepsEverything() {
        create(schema(4))
        insertVersion3Rows()
        db.createStatement().use { it.executeUpdate("INSERT INTO song_styles (trackKey, modelVersion, top) VALUES ('Music/a.mp3', 1, x'0100ffff')") }
        run(DatabaseMigrations.SQL_4_TO_5)

        assertEquals(storedStatements(5), tables())
        assertColumnsMatch(5)
        assertVersion3RowsKept()
        assertEquals(1, count("song_styles"))
        db.createStatement().use {
            it.execute("INSERT INTO song_loudness (trackKey, version, integratedLufs, samplePeak, measuredAt) VALUES ('Music/a.mp3', 1, -9.5, 0.98, 3)")
            it.executeQuery("SELECT integratedLufs FROM song_loudness WHERE trackKey = 'Music/a.mp3'").use { row ->
                assertTrue(row.next())
                assertEquals(-9.5, row.getDouble(1), 0.0)
            }
        }
    }

    @Test
    fun threeToFiveRunsBothAndEndsAtVersionFive() {
        create(schema(3))
        insertVersion3Rows()
        run(DatabaseMigrations.SQL_3_TO_4)
        run(DatabaseMigrations.SQL_4_TO_5)

        assertEquals(storedStatements(5), tables())
        assertColumnsMatch(5)
        assertVersion3RowsKept()
        // Nothing is analysed for styles or loudness yet, so the worker gives every song one full decode.
        assertEquals(0, count("song_styles"))
        assertEquals(0, count("song_loudness"))
    }

    @Test
    fun versionFourOnlyAddsTheStylesTable() {
        val before = createStatements(schema(3))
        val after = createStatements(schema(4))
        assertEquals(before, after - "song_styles")
        assertEquals(setOf("song_styles"), after.keys - before.keys)
    }

    @Test
    fun versionFiveOnlyAddsTheLoudnessTable() {
        val before = createStatements(schema(4))
        val after = createStatements(schema(5))
        assertEquals(before, after - "song_loudness")
        assertEquals(setOf("song_loudness"), after.keys - before.keys)
    }

    @Test
    fun theMigrationsMatchWhatRoomWouldCreate() {
        val strip = { sql: String -> sql.replace("CREATE TABLE IF NOT EXISTS", "").trim() }
        assertEquals(strip(createStatements(schema(4)).getValue("song_styles")), strip(DatabaseMigrations.SQL_3_TO_4.single()))
        assertEquals(strip(createStatements(schema(5)).getValue("song_loudness")), strip(DatabaseMigrations.SQL_4_TO_5.single()))
        assertEquals(strip(createStatements(schema(6)).getValue("song_transitions")), strip(DatabaseMigrations.SQL_5_TO_6.single()))
        assertEquals(
            listOf("eq_settings", "eq_presets").map { strip(createStatements(schema(7)).getValue(it)) },
            DatabaseMigrations.SQL_6_TO_7.map(strip),
        )
    }

    /** 7 → 8 adds a column, which SQLite can't do twice; Room never runs a migration twice either. */
    @Test
    fun eachMigrationRunsTwiceWithoutHarm() {
        create(schema(3))
        run(DatabaseMigrations.SQL_3_TO_4)
        run(DatabaseMigrations.SQL_3_TO_4)
        assertEquals(createStatements(schema(4)).keys, tables().keys)
        run(DatabaseMigrations.SQL_4_TO_5)
        run(DatabaseMigrations.SQL_4_TO_5)
        assertEquals(createStatements(schema(5)).keys, tables().keys)
        run(DatabaseMigrations.SQL_5_TO_6)
        run(DatabaseMigrations.SQL_5_TO_6)
        assertEquals(createStatements(schema(6)).keys, tables().keys)
        run(DatabaseMigrations.SQL_6_TO_7)
        run(DatabaseMigrations.SQL_6_TO_7)
        assertEquals(createStatements(schema(7)).keys, tables().keys)
    }
}
