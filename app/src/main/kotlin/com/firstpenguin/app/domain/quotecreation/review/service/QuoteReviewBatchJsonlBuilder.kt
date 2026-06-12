package com.firstpenguin.app.domain.quotecreation.review.service

import com.firstpenguin.app.domain.book.model.Book
import com.firstpenguin.app.domain.quotebatch.model.QuoteBatchModelVersion
import com.firstpenguin.app.domain.quotecreation.review.model.QuoteCandidate
import com.firstpenguin.app.domain.quotecreation.review.model.QuoteReviewBatchTarget
import org.springframework.stereotype.Component
import tools.jackson.databind.json.JsonMapper

private val QUOTE_CANDIDATE_REVIEW_PROMPT_GUIDE =
    """
너는 감정 기반 문장 추천 서비스의 후보 인용구 검수기다.

목표:
- 제공된 후보 문장 중 추천 문장으로 저장할 만한 것만 최대 3개 고른다.
- 반드시 제공된 candidateId 중에서만 선택한다.
- 후보 문장을 새로 만들거나, 요약하거나, 다듬거나, 번역하지 않는다.
- 쓸 만한 후보가 없으면 acceptedCandidateIds를 빈 배열로 둔다.
- 책마다 하나를 골라야 한다고 생각하지 마라. 의심스러우면 선택하지 않는다.
- 출력은 JSON schema만 따른다.

[판단 태도]
- 이 작업은 좋은 후보를 찾는 일이 아니라, 나쁜 후보를 대부분 걸러내는 일이다.
- "괜찮을 수도 있다"는 후보는 제외한다.
- "앱에 오늘의 문장처럼 단독 노출해도 자연스럽다"는 확신이 있을 때만 선택한다.
- 후보 문장 내용만 검수한다. 책 제목, 저자, isbn, 링크는 책 소개문인지 판단하는 보조 정보로만 쓴다.

[선택 기준]
아래 조건을 모두 만족하는 후보만 선택한다.
1. 한국어 문장으로 자연스럽다.
2. 10~90자 정도의 단독 문장으로 앱 화면에 그대로 노출해도 어색하지 않다.
3. 감정, 생각, 태도, 깨달음, 위로, 성찰, 용기, 관점 전환 중 하나와 자연스럽게 연결된다.
4. 특정 책, 줄거리, 등장인물, 장면을 몰라도 한 문장만으로 뜻이 전달된다.
5. 표현이 구체적이고 기억될 만한 여운이 있다.
6. 흔한 자기계발 문구, AI 생성문, 책 소개문처럼 느껴지지 않는다.
7. 앞뒤 따옴표만 붙은 경우에는 따옴표를 무시하고 내용 자체로 판단한다.

[강한 제외 기준]
아래에 하나라도 해당하면 반환하지 마라.
- 영어, 러시아어, 일본어 등 외국어 문장
- 한자, 특수문자, 깨진 문자가 섞여 어색한 문장
- 번호, 출처 표기, 불필요한 기호가 포함된 문장
- 리뷰, 서평, 홍보, 추천사, 독자 감상, 줄거리 요약, 해설 문장
- “읽어버린 책”, “대표작”, “이 책은”, “작가는”, “우리에게 제시한다”, “생각하게 만든다”처럼 책을 평가하거나 소개하는 문장
- “자연의 소중함”, “새로운 삶의 방식”, “잊혀진 과거”처럼 책의 주제나 교훈을 설명하는 문장
- 설명문, 교과서 문장, 정보 전달 문장, 논설문 문장
- 특정 인물명, 가족 호칭, 등장인물 관계를 알아야 이해되는 문장
- 그, 그녀, 그대, 계화처럼 특정 인물이나 관계 맥락이 있어야 자연스러운 문장
- 특정 장면 설명, 단순 묘사, 사건 설명, 대사 조각
- 범죄, 살인, 피해자, 사탄, 질병처럼 추천 문장으로 부담이 큰 표현
- 여러 문장을 이어 붙인 긴 발췌문
- 마침표, 물음표, 느낌표 기준으로 2문장 이상인 문장
- “인생은 결국”, “사랑은 결국”, “우리는 모두”, “인간은 누구나”처럼 흔한 격언 말투
- “중요합니다”, “있습니다”, “제시한다”, “독려합니다”, “생각하게 만든다”처럼 보고서나 책 소개 말투
- “~것을 깨달았다”, “~것이 중요하다”, “~수 있다”, “~해야 한다”처럼 AI 요약문이나 교훈문처럼 보이는 문장
- “인생은 선택의 연속이다”, “인생은 아름다운 여행입니다”처럼 너무 일반적인 문장

[경계 사례]
- “그럼에도 불구하고, 나는 이 세상을 사랑한다.”처럼 짧고 독립적인 정서가 있으면 선택할 수 있다.
- “안녕은 결국 나의 선택이었다.”처럼 구체적이고 단독으로 여운이 있으면 선택할 수 있다.
- “눈물은 우리가 가슴에 담을 수 있는 가장 무거운 물건이다.”처럼 비유가 선명하면 선택할 수 있다.
- “인생은 결국 혼자 사는 것이다.”처럼 흔하고 납작한 단정은 제외한다.
- “그녀는 자신의 삶을 위해...”처럼 인물과 줄거리 맥락이 드러나면 제외한다.
- “대온실은 우리에게...”처럼 책 제목이나 작품 주제를 설명하면 제외한다.

[형식 기준]
- acceptedCandidateIds에는 선택한 candidateId 정수만 넣는다.
- 좋은 후보가 1개면 1개만 선택한다.
- 억지로 3개를 채우지 않는다.
- 후보가 모두 별로면 빈 배열을 반환한다.
- 같은 의미를 반복하는 후보가 여러 개면 가장 자연스러운 1개만 선택한다.
    """.trimIndent()

@Component
class QuoteReviewBatchJsonlBuilder(
    private val jsonMapper: JsonMapper,
) {
    fun build(targets: List<QuoteReviewBatchTarget>): String =
        targets.joinToString("\n") { target ->
            jsonMapper.writeValueAsString(
                mapOf(
                    "custom_id" to "book-${target.book.id}",
                    "method" to "POST",
                    "url" to "/v1/responses",
                    "body" to reviewRequestBody(target),
                ),
            )
        }
}

private fun reviewRequestBody(target: QuoteReviewBatchTarget): Map<String, Any> =
    mapOf(
        "model" to QuoteBatchModelVersion.QUOTE_REVIEW_V1.model,
        "reasoning" to mapOf("effort" to "low"),
        "input" to buildReviewPrompt(target),
        "text" to
            mapOf(
                "format" to quoteReviewSchema(target.book, target.candidates),
                "verbosity" to "low",
            ),
    )

private fun buildReviewPrompt(target: QuoteReviewBatchTarget): String =
    listOf(
        QUOTE_CANDIDATE_REVIEW_PROMPT_GUIDE,
        reviewBookText(target.book),
        candidateText(target.candidates),
    ).joinToString("\n\n")

private fun reviewBookText(book: Book): String =
    """
    [대상 책]
    bookId: ${book.id}
    title: ${book.title}
    author: ${book.author}
    isbn13: ${book.isbn13}
    aladinLink: ${book.aladinLink}
    """.trimIndent()

private fun candidateText(candidates: List<QuoteCandidate>): String =
    candidates.joinToString(
        separator = "\n",
        prefix = "[후보 문장]\n",
    ) { candidate ->
        "- candidateId: ${candidate.id}\n  content: ${candidate.content}"
    }
