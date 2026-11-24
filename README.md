# JReview

JReview is a tiny Java/Spring Boot backend that classifies e-commerce reviews with the Gemini API. It returns one of three labels: `VERIFIED`, `SPAM`, or `TOXIC`.

It is intentionally small. There is one endpoint, six main classes, no database, no login, and no frontend. The goal is that you can read the whole project in a few minutes and explain it out loud.

---

## 1. What the project does

Send review text to JReview. JReview asks Gemini Decision 1 to pick one label.

| Label | Meaning | Example |
|---|---|---|
| `VERIFIED` | A genuine first-hand opinion about the product. Positive **or negative**. | “Broke after two days. Very disappointed.” |
| `SPAM` | Promotion, affiliate links, giveaways, phishing, or fake content. | “Click my link to win a free iPhone!!” |
| `TOXIC` | Harassment, hate, threats, or personal abuse. | “Anyone who bought this is an idiot.” |

Two important clarifications:

1. **Negative reviews are not toxic or spam.** A real complaint is `VERIFIED`.
2. **`VERIFIED` does not mean verified purchase.** It only means the comment reads as a genuine first-hand opinion.

Out of scope for this first version: databases, authentication, queues, frontend, purchase verification, confidence scores, and free-text explanations.

---

## 2. Why Gemini Decision 1

JReview used to be planned around a chat model. It now uses **Gemini Decision 1**, a model built specifically for classification.

| Chat model | Decision model |
|---|---|
| Generates text you must parse | Returns typed answers directly |
| Output tokens cost/latency matter | Zero output tokens |
| Good for summaries and explanations | Good for labels, yes/no, and scores |

Gemini Decision 1 returns probabilities and a chosen label. JReview only reads the chosen label, so there is no JSON parsing of model-written text.

Because of that, the API response is intentionally minimal:

```json
{
  "verdict": "VERIFIED"
}
```

There is no `reason` field. That keeps the code short and the explanation clean.

---

## 3. Architecture

```text
          HTTP POST /api/v1/reviews/classify
                        │
                        ▼
              ReviewController
              (reads JSON, validates text)
                        │
                        ▼
              ModerationService
              (owns the moderation rules)
                        │
                        ▼
             GeminiDecisionClient
             (builds request, calls Gemini)
                        │
                        ▼
        Gemini API — gemini-decision-1
        POST /v1beta/decisions
```

The important idea: **each class has one job**.

- The controller does not know Gemini.
- The service does not know HTTP.
- The client does not know what the labels mean.

---

## 4. Project structure

### Main code

| File | Job |
|---|---|
| `JReviewApplication.java` | Starts the Spring Boot app. |
| `Verdict.java` | Defines the three allowed labels. |
| `ReviewController.java` | Exposes the REST endpoint and validates input. |
| `ModerationService.java` | Holds the moderation rules and converts Gemini’s label to `Verdict`. |
| `GeminiDecisionClient.java` | Sends one request to Gemini and returns the chosen label. |
| `ApiExceptionHandler.java` | Converts errors into short HTTP responses. |

### Tests

| File | What it proves |
|---|---|
| `GeminiDecisionClientTest` | Sends the API key header and parses Gemini’s answer. |
| `ModerationServiceTest` | Maps all three labels correctly and rejects unknown labels. |
| `ReviewControllerTest` | Rejects blank text and returns a verdict for valid text. |
| `JReviewApplicationTests` | The Spring context starts. |

### Config

| File | Job |
|---|---|
| `application.properties` | Holds non-secret config and reads the key from the environment. |
| `.env.example` | Shows the environment variable name. The real `.env` is gitignored. |

---

## 5. Request flow, step by step

1. A client sends `POST /api/v1/reviews/classify` with `{"text": "..."}`.
2. `ReviewController` checks that `text` is not blank.
3. `ReviewController` calls `ModerationService.moderate(text)`.
4. `ModerationService` supplies the moderation rules as a question with three choices.
5. `GeminiDecisionClient` builds a decision request and posts it to Gemini.
6. Gemini returns `answers.verdict.choice`, for example `"TOXIC"`.
7. `ModerationService` converts that string to `Verdict.TOXIC`.
8. `ReviewController` returns `{"verdict": "TOXIC"}`.

---

## 6. Moderation rules

The rule order is written directly into the question sent to Gemini:

1. **TOXIC first** — harassment, hate, threats, or personal abuse.
2. **SPAM second** — promotions, affiliate links, giveaways, phishing, or fake content.
3. **VERIFIED otherwise** — a genuine first-hand opinion.

The prompt explicitly says:

> A negative review is still VERIFIED if it is a real experience.

Why this matters: “This product is terrible and broke immediately” is `VERIFIED`, not `TOXIC`. Deciding otherwise would punish honest criticism.

Unknown labels do **not** become `VERIFIED`. They cause a `502` response. This prevents a bad model response from silently approving content.

---

## 7. The Gemini request

`GeminiDecisionClient` builds this shape:

```json
{
  "model": "gemini-decision-1",
  "input": "This product broke after two days. Very disappointed.",
  "questions": {
    "verdict": {
      "type": "choice",
      "instructions": "Classify this e-commerce review...",
      "criteria": {
        "TOXIC": "Harassment, hate, threats, or personal abuse.",
        "SPAM": "Promotion, affiliate links, giveaways, phishing, or fake content.",
        "VERIFIED": "A genuine first-hand opinion. Positive or negative."
      }
    }
  }
}
```

It sends:

```text
POST https://generativelanguage.googleapis.com/v1beta/decisions
x-goog-api-key: $GEMINI_API_KEY
Content-Type: application/json
```

It reads only:

```text
answers.verdict.choice
```

That is the whole API call function.

---

## 8. Tech stack and why

| Choice | Why |
|---|---|
| Java 25 | Installed locally and current. |
| Spring Boot 4.2.0 | Gives us REST handling with almost no boilerplate. |
| Spring `RestClient` | One readable HTTP call, no SDK dependency. |
| Gemini Decision 1 | Purpose-built, fast classification with zero output tokens. |
| Maven wrapper | Works even when Maven is not installed system-wide. |
| JUnit + Mockito + MockRestServiceServer | Tests run locally without a real API key. |

---

## 9. Prerequisites

- Java 25
- A Gemini API key from [Google AI Studio](https://aistudio.google.com/api-keys)
- `curl` for testing
- Git and GitHub CLI if you want to push the repository

---

## 10. Run it locally

Create a local `.env` file if you want, or simply export the variable:

```bash
export GEMINI_API_KEY="your-new-key-here"
./mvnw spring-boot:run
```

If you use `.env`:

```bash
GEMINI_API_KEY=your-new-key-here
```

Then load it:

```bash
set -a
source .env
set +a
./mvnw spring-boot:run
```

The app starts on `http://localhost:8080`.

---

## 11. API reference

### Classify a review

```bash
curl -X POST http://localhost:8080/api/v1/reviews/classify \
  -H "Content-Type: application/json" \
  -d '{"text":"This product broke after two days. Very disappointed."}'
```

Success:

```json
{
  "verdict": "VERIFIED"
}
```

Another example:

```bash
curl -X POST http://localhost:8080/api/v1/reviews/classify \
  -H "Content-Type: application/json" \
  -d '{"text":"Visit my site for a free iPhone giveaway!"}'
```

Response:

```json
{
  "verdict": "SPAM"
}
```

### Errors

Blank text:

```json
{
  "timestamp": "...",
  "status": 400,
  "error": "Bad Request"
}
```

Gemini unavailable, bad key, rate limit, or malformed label:

```json
Gemini moderation is unavailable
```

with HTTP status `502`.

---

## 12. Configuration

Defined in `src/main/resources/application.properties`:

| Property | Default | Purpose |
|---|---|---|
| `gemini.api.key` | `${GEMINI_API_KEY:}` | Secret key sent in `x-goog-api-key`. |
| `gemini.api.base-url` | `https://generativelanguage.googleapis.com` | Gemini host. |
| `gemini.api.model` | `gemini-decision-1` | The classification model. |

The empty default for the key exists so tests can start without secrets. Real classification still requires a valid key.

---

## 13. Testing

Run:

```bash
./mvnw clean verify
```

What is tested:

- The Gemini client sends the expected path, header, and body.
- A canned Gemini answer is parsed correctly.
- `TOXIC`, `SPAM`, and `VERIFIED` map to the enum.
- An unknown label throws an exception.
- Blank text returns `400`.
- Valid text returns `200`.
- The Spring context starts.

No real Gemini call happens in tests. No API key is required.

---

## 14. Costs and free tier

For `gemini-decision-1`:

- Free tier: input tokens are free.
- Output tokens: always zero.
- Paid tier, if ever needed: `$0.10` per 1M input tokens.
- Rate limits still apply. Check them in AI Studio.

Caveats:

- The free tier is rate-limited, not unlimited.
- On the free tier, content may be used to improve Google’s products.
- Avoid sending real customer PII on the free tier.
- If you hit a limit, Gemini returns an error and JReview responds with `502`.

---

## 15. Security notes

- The key is read from `GEMINI_API_KEY`; it is never hardcoded.
- `.env` is gitignored.
- The key is sent as a header, not in the URL.
- JReview does not log the key.
- If a key was ever pasted into chat or a ticket, rotate it in Google AI Studio.
- For production, add authentication, request size limits, and abuse protection.

---

## 16. Limitations

This first version does not:

- verify purchases or orders;
- store reviews;
- provide model-written explanations;
- handle multiple languages explicitly;
- provide a human review queue;
- protect against abuse or high traffic;
- add confidence thresholds.

Possible next steps:

1. Add confidence thresholds from Gemini’s `confidence` field.
2. Send low-confidence reviews to human review.
3. Add request logging without storing review text.
4. Add Spring Security.
5. Add a database only if you need an audit trail.
6. Add a GitHub Actions workflow running `./mvnw -B verify`.

---

## 17. Explain this project in 5 minutes

> “JReview is a Spring Boot service with one endpoint. The controller validates the review text. The service owns the moderation rules: toxic first, spam second, verified otherwise. A negative but genuine review is still verified. The client makes one HTTP call to Gemini Decision 1, which is built for classification and generates zero output tokens. We read the chosen label, convert it to an enum, and return it as JSON. The API key comes from an environment variable and never appears in code. Tests stub Gemini, so CI needs no secrets.”

---

## 18. Troubleshooting

| Problem | Likely cause | Fix |
|---|---|---|
| App starts but requests return `502` | Missing or invalid `GEMINI_API_KEY` | Export a fresh key and restart. |
| `429` from Gemini | Rate limit reached | Wait, reduce traffic, or check AI Studio limits. |
| Blank text returns `400` | Validation worked as intended | Send non-empty `text`. |
| Unknown label returns `502` | Gemini returned an unexpected value | Check model/config; JReview refuses to guess. |
| Tests fail before reaching Gemini | Java or Maven wrapper issue | Use Java 25 and run `./mvnw clean verify`. |
