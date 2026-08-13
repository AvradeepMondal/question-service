package com.avradeep.QuestionService.util.extractor;

import com.avradeep.QuestionService.dto.ExtractedDocument;
import com.avradeep.QuestionService.entity.FileType;

import java.io.IOException;
import java.util.Set;

public interface DocumentExtractor {

//    /**
//     * Extract textual content from the document.
//     */
//    String extractText(String filePath);
//
//    /**
//     * Returns the supported document type.
//     */
      Set<FileType> supports();

      ExtractedDocument extract(String filePath) throws IOException;
}
