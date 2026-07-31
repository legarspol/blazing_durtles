package com.smouldering_durtles.wk.db.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.fasterxml.jackson.core.type.TypeReference
import com.smouldering_durtles.wk.api.model.ApiStage
import com.smouldering_durtles.wk.db.Converters
import com.smouldering_durtles.wk.util.ObjectSupport.isEmpty
import java.io.IOException

/**
 * Room entity for the srs_system table. This represents an SRS system.
 */
@Entity(tableName = "srs_system")
class SrsSystemDefinition {
    /**
     * The system's unique ID
     */
    @JvmField @PrimaryKey var id: Long = 0L

    /**
     * The name.
     */
    @JvmField var name: String? = null

    /**
     * A description.
     */
    @JvmField var description: String? = null

    /**
     * The stages in the system, encoded as a JSON string.
     */
    @JvmField var stages: String? = null

    /**
     * The initiate stage.
     */
    @JvmField var unlockingStagePosition: Long = 0L

    /**
     * The first post-initiate stage.
     */
    @JvmField var startingStagePosition: Long = 0L

    /**
     * The first passing stage.
     */
    @JvmField var passingStagePosition: Long = 0L

    /**
     * The burned stage.
     */
    @JvmField var burningStagePosition: Long = 0L

    /**
     * Get the parsed version of the stages list.
     *
     * @return the list
     */
    fun getParsedStages(): List<ApiStage> {
        val json = stages
        if (isEmpty(json)) {
            return emptyList()
        }
        return try {
            Converters.getObjectMapper().readValue(json, object : TypeReference<List<ApiStage>>() {})
        } catch (e: IOException) {
            emptyList()
        }
    }
}
