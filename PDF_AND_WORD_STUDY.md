# PDF and original-language study roadmap

## PDF reader
Implemented in the starter: Android document picker, `PdfRenderer`, page navigation, and basic zoom.

Not implemented yet: PDF text extraction/search, text selection/copying, annotations, highlights, saved bookmarks, and robust large-document caching. `PdfRenderer` renders pages; it does not itself provide a complete PDF text search or annotation engine.

## Greek and Hebrew word study
The current app displays a few illustrative entries only. A real implementation needs legally redistributable lexical and morphology datasets plus verse-level word alignment. A dictionary alone cannot reliably map every English word to a Greek/Hebrew lemma.

Potential data fields:
- language, lemma, original script, transliteration
- Strong's number (only when verified)
- part of speech, gloss, definition, morphology
- source/licence and attribution
- verse-level word alignment and token position

Never fabricate Strong's numbers, morphology, or lexical definitions. Track licences and attribution for each dataset.
