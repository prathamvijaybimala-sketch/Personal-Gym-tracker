package com.gympro.app.domain

/**
 * AI prompt generator — the app's signature feature. The prompt text below is
 * byte-for-byte identical to the web app's template (including em/en dashes and
 * the ellipsis), with only the user's routine type interpolated.
 */
object AIPromptBuilder {

    fun build(routineType: String): String = """Generate a ${routineType} gym workout routine for me.

Return ONLY a valid JSON object — no explanation, no markdown, no extra text. The JSON must follow this exact format:

{
  "name": "Routine Name",
  "days": [
    {
      "day": 1,
      "name": "PUSH",
      "exercises": [
        {
          "name": "Bench Press",
          "sets": "3",
          "reps": "10",
          "link": "https://exrx.net/..."
        }
      ]
    },
    {
      "day": 0,
      "name": "REST",
      "exercises": []
    }
  ]
}

Rules:
- "day" must be 0–6 (0 = Sunday, 1 = Monday, … 6 = Saturday)
- Include all 7 days (0–6). Use "REST" with empty exercises for rest days
- "sets" and "reps" are strings (e.g. "3" and "10")
- "link" should be a real exrx.net or strengthlog.com URL for the exercise, or an empty string ""
- Include 3–6 exercises per training day
- Routine type: ${routineType}"""
}
