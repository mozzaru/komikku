package xyz.nulldev.ts.api.http.serializer

import eu.kanade.tachiyomi.animesource.model.AnimeFilter
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonObjectBuilder
import kotlinx.serialization.json.add
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlin.reflect.KClass
import kotlin.reflect.KProperty1

interface Serializer<in T : AnimeFilter<out Any?>> {
    fun JsonObjectBuilder.serialize(filter: T) {}
    fun deserialize(json: JsonObject, filter: T) {}

    /**
     * Automatic two-way mappings between fields and JSON
     */
    fun mappings(): List<Pair<String, KProperty1<in T, *>>> = emptyList()

    val serializer: FilterSerializer
    val type: String
    val clazz: KClass<in T>
}

class HeaderSerializer(override val serializer: FilterSerializer) : Serializer<AnimeFilter.Header> {
    override val type = "HEADER"
    override val clazz = AnimeFilter.Header::class

    override fun mappings() = listOf(
        Pair(NAME, AnimeFilter.Header::name),
    )

    companion object {
        const val NAME = "name"
    }
}

class SeparatorSerializer(override val serializer: FilterSerializer) : Serializer<AnimeFilter.Separator> {
    override val type = "SEPARATOR"
    override val clazz = AnimeFilter.Separator::class

    override fun mappings() = listOf(
        Pair(NAME, AnimeFilter.Separator::name),
    )

    companion object {
        const val NAME = "name"
    }
}

class SelectSerializer(override val serializer: FilterSerializer) : Serializer<AnimeFilter.Select<Any>> {
    override val type = "SELECT"
    override val clazz = AnimeFilter.Select::class

    override fun JsonObjectBuilder.serialize(filter: AnimeFilter.Select<Any>) {
        // Serialize values to JSON
        putJsonArray(VALUES) {
            filter.values.map {
                it.toString()
            }.forEach { add(it) }
        }
    }

    override fun mappings() = listOf(
        Pair(NAME, AnimeFilter.Select<Any>::name),
        Pair(STATE, AnimeFilter.Select<Any>::state),
    )

    companion object {
        const val NAME = "name"
        const val VALUES = "values"
        const val STATE = "state"
    }
}

class TextSerializer(override val serializer: FilterSerializer) : Serializer<AnimeFilter.Text> {
    override val type = "TEXT"
    override val clazz = AnimeFilter.Text::class

    override fun mappings() = listOf(
        Pair(NAME, AnimeFilter.Text::name),
        Pair(STATE, AnimeFilter.Text::state),
    )

    companion object {
        const val NAME = "name"
        const val STATE = "state"
    }
}

class CheckboxSerializer(override val serializer: FilterSerializer) : Serializer<AnimeFilter.CheckBox> {
    override val type = "CHECKBOX"
    override val clazz = AnimeFilter.CheckBox::class

    override fun mappings() = listOf(
        Pair(NAME, AnimeFilter.CheckBox::name),
        Pair(STATE, AnimeFilter.CheckBox::state),
    )

    companion object {
        const val NAME = "name"
        const val STATE = "state"
    }
}

class TriStateSerializer(override val serializer: FilterSerializer) : Serializer<AnimeFilter.TriState> {
    override val type = "TRISTATE"
    override val clazz = AnimeFilter.TriState::class

    override fun mappings() = listOf(
        Pair(NAME, AnimeFilter.TriState::name),
        Pair(STATE, AnimeFilter.TriState::state),
    )

    companion object {
        const val NAME = "name"
        const val STATE = "state"
    }
}

class GroupSerializer(override val serializer: FilterSerializer) : Serializer<AnimeFilter.Group<Any?>> {
    override val type = "GROUP"
    override val clazz = AnimeFilter.Group::class

    override fun JsonObjectBuilder.serialize(filter: AnimeFilter.Group<Any?>) {
        putJsonArray(STATE) {
            filter.state.forEach {
                add(
                    if (it is AnimeFilter<*>) {
                        @Suppress("UNCHECKED_CAST")
                        serializer.serialize(it as AnimeFilter<Any?>)
                    } else {
                        JsonNull
                    },
                )
            }
        }
    }

    override fun deserialize(json: JsonObject, filter: AnimeFilter.Group<Any?>) {
        json[STATE]!!.jsonArray.forEachIndexed { index, jsonElement ->
            if (jsonElement !is JsonNull) {
                @Suppress("UNCHECKED_CAST")
                serializer.deserialize(filter.state[index] as AnimeFilter<Any?>, jsonElement.jsonObject)
            }
        }
    }

    override fun mappings() = listOf(
        Pair(NAME, AnimeFilter.Group<Any?>::name),
    )

    companion object {
        const val NAME = "name"
        const val STATE = "state"
    }
}

class SortSerializer(override val serializer: FilterSerializer) : Serializer<AnimeFilter.Sort> {
    override val type = "SORT"
    override val clazz = AnimeFilter.Sort::class

    override fun JsonObjectBuilder.serialize(filter: AnimeFilter.Sort) {
        // Serialize values
        putJsonArray(VALUES) {
            filter.values.forEach { add(it) }
        }
        // Serialize state
        put(
            STATE,
            filter.state?.let { (index, ascending) ->
                buildJsonObject {
                    put(STATE_INDEX, index)
                    put(STATE_ASCENDING, ascending)
                }
            } ?: JsonNull,
        )
    }

    override fun deserialize(json: JsonObject, filter: AnimeFilter.Sort) {
        // Deserialize state
        filter.state = (json[STATE] as? JsonObject)?.let {
            AnimeFilter.Sort.Selection(
                it[STATE_INDEX]!!.jsonPrimitive.int,
                it[STATE_ASCENDING]!!.jsonPrimitive.boolean,
            )
        }
    }

    override fun mappings() = listOf(
        Pair(NAME, AnimeFilter.Sort::name),
    )

    companion object {
        const val NAME = "name"
        const val VALUES = "values"
        const val STATE = "state"

        const val STATE_INDEX = "index"
        const val STATE_ASCENDING = "ascending"
    }
}

class AutoCompleteSerializer(override val serializer: FilterSerializer) : Serializer<AnimeFilter.AutoComplete> {
    override val type = "AUTOCOMPLETE"
    override val clazz = AnimeFilter.AutoComplete::class

    override fun JsonObjectBuilder.serialize(filter: AnimeFilter.AutoComplete) {
        // Serialize values to JSON
        putJsonArray(STATE) {
            filter.state.forEach { add(it) }
        }
    }

    override fun deserialize(json: JsonObject, filter: AnimeFilter.AutoComplete) {
        // Deserialize state
        filter.state = json[STATE]!!.jsonArray.map {
            it.jsonPrimitive.content
        }
    }

    override fun mappings() = listOf(
        Pair(NAME, AnimeFilter.AutoComplete::name),
    )

    companion object {
        const val NAME = "name"
        const val STATE = "state"
    }
}
