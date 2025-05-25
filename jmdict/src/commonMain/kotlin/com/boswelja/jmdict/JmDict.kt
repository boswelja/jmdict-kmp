package com.boswelja.jmdict

import io.github.boswelja.jmdict.jmdict.generated.resources.Res
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import nl.adaptivity.xmlutil.ExperimentalXmlUtilApi
import nl.adaptivity.xmlutil.serialization.XML
import java.util.zip.GZIPInputStream

@OptIn(ExperimentalXmlUtilApi::class)
internal val Serializer = XML {
    defaultPolicy {
        pedantic = true
        autoPolymorphic = true
        throwOnRepeatedElement = true
        isStrictBoolean = true
        isStrictAttributeNames = true
        isXmlFloat = true
        verifyElementOrder = true
    }
}

suspend fun streamJmDict(): Sequence<Entry> {
    val compressedBytes = withContext(Dispatchers.IO) {
        Res.readBytes("files/jmdict.xml")
    }
    return GZIPInputStream(compressedBytes).bufferedReader()
        .lineSequence()
        .asEntrySequence()
}

internal fun Sequence<String>.asEntrySequence(): Sequence<Entry> {
    return this
        .dropWhile { !it.contains("<entry>") }
        .chunkedUntil { it.contains("<entry>") }
        .chunked(100)
        .flatMap { entryLines ->
            if (entryLines.isNotEmpty()) {
                Serializer.decodeFromString<JMdict>("<JMdict>${entryLines.flatten().joinToString(separator = "")}</JMdict>").entrys
            } else emptyList()
        }
}
