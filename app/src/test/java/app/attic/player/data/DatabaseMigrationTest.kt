package app.attic.player.data

import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import java.sql.Connection
import java.sql.DriverManager

/**
 * Runs the 3 to 4 migration on real SQLite: a database built from Room's exported version 3 schema,
 * with data in it, must end up with exactly the tables Room exported for version 4, data intact.
 */
class DatabaseMigrationTest {
    private val db: Connection = DriverManager.getConnection("jdbc:sqlite::memory:")

    @After
    fun close() = db.close()

    private fun schema(version: Int): JSONObject =
        JSONObject(File("schemas/app.attic.player.data.AtticDatabase/$version.json").readText()).getJSONObject("database")

    /** Table name to its CREATE statement, as Room would run it. */
    private fun createStatements(schema: JSONObject): Map<String, String> {
        val entities = schema.getJSONArray("entities")
        return (0 until entities.length()).map(entities::getJSONObject).associate { entity ->
            val name = entity.getString("tableName")
            name to entity.getString("createSql").replace("\${TABLE_NAME}", name)
        }
    }

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

    private fun tables(): Map<String, String> = db.createStatement().use { statement ->
        statement.executeQuery("SELECT name, sql FROM sqlite_master WHERE type = 'table' AND name NOT LIKE 'sqlite_%'").use { rows ->
            buildMap { while (rows.next()) put(rows.getString(1), rows.getString(2)) }
        }
    }

    @Test
    fun threeToFourAddsStylesAndKeepsAnalysis() {
        create(schema(3))
        val embedding = byteArrayOf(1, 2, 3, 4)
        db.prepareStatement("INSERT INTO song_analysis (trackKey, modelVersion, embedding, chroma, analyzedAt) VALUES (?, 1, ?, NULL, 5)").use {
            it.setString(1, "Music/a.mp3")
            it.setBytes(2, embedding)
            it.executeUpdate()
        }
        db.createStatement().use { s -> DatabaseMigrations.SQL_3_TO_4.forEach(s::execute) }

        val expected = createStatements(schema(4)).mapValues { (_, sql) -> sql.removePrefix("CREATE TABLE IF NOT EXISTS ").let { "CREATE TABLE $it" } }
        assertEquals(expected, tables())

        db.createStatement().use { s ->
            s.executeQuery("SELECT trackKey, embedding FROM song_analysis").use { rows ->
                rows.next()
                assertEquals("Music/a.mp3", rows.getString(1))
                assertArrayEquals(embedding, rows.getBytes(2))
            }
            s.executeUpdate("INSERT INTO song_styles (trackKey, modelVersion, top) VALUES ('Music/a.mp3', 1, x'0100ffff')")
            s.executeQuery("SELECT COUNT(*) FROM song_styles").use { rows ->
                rows.next()
                assertEquals(1, rows.getInt(1))
            }
        }
    }

    @Test
    fun versionFourOnlyAddsTheStylesTable() {
        val before = createStatements(schema(3))
        val after = createStatements(schema(4))
        assertEquals(before, after - "song_styles")
        assertEquals(setOf("song_styles"), after.keys - before.keys)
    }

    @Test
    fun migrationRunsTwiceWithoutHarm() {
        create(schema(3))
        db.createStatement().use { s ->
            DatabaseMigrations.SQL_3_TO_4.forEach(s::execute)
            DatabaseMigrations.SQL_3_TO_4.forEach(s::execute)
        }
        assertEquals(createStatements(schema(4)).keys, tables().keys)
    }
}
