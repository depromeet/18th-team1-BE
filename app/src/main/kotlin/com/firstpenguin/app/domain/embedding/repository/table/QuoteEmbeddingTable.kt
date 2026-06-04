package com.firstpenguin.app.domain.embedding.repository.table

import org.jooq.impl.DSL
import java.time.LocalDateTime

internal object QuoteEmbeddingTable {
    val QUOTE_EMBEDDINGS = DSL.table(DSL.name("quote_embeddings"))
    val ID = DSL.field(DSL.name("quote_embeddings", "id"), Long::class.java)
    val QUOTE_ID = DSL.field(DSL.name("quote_embeddings", "quote_id"), Long::class.java)
    val EMBEDDING_MODEL = DSL.field(DSL.name("quote_embeddings", "embedding_model"), String::class.java)
    val EMBEDDING = DSL.field(DSL.name("quote_embeddings", "embedding"), String::class.java)
    val EMBEDDING_TEXT_HASH = DSL.field(DSL.name("quote_embeddings", "embedding_text_hash"), String::class.java)
    val CREATED_AT = DSL.field(DSL.name("quote_embeddings", "created_at"), LocalDateTime::class.java)
    val UPDATED_AT = DSL.field(DSL.name("quote_embeddings", "updated_at"), LocalDateTime::class.java)
}
