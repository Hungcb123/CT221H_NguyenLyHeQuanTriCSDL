package com.ctu.freshgrocery.service.impl;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
public class BertTokenizer {

    private static final Pattern DIACRITICS_PATTERN = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
    private static final String CLS_TOKEN = "[CLS]";
    private static final String SEP_TOKEN = "[SEP]";
    private static final String UNK_TOKEN = "[UNK]";
    private static final String PAD_TOKEN = "[PAD]";

    private final Map<String, Long> vocab = new HashMap<>();
    private long clsId = 101L;
    private long sepId = 102L;
    private long unkId = 100L;
    private long padId = 0L;

    @Getter
    public static class TokenizedInput {
        private final long[][] inputIds;
        private final long[][] attentionMask;
        private final long[][] tokenTypeIds;
        private final int sequenceLength;

        public TokenizedInput(long[][] inputIds, long[][] attentionMask, long[][] tokenTypeIds, int sequenceLength) {
            this.inputIds = inputIds;
            this.attentionMask = attentionMask;
            this.tokenTypeIds = tokenTypeIds;
            this.sequenceLength = sequenceLength;
        }
    }

    public BertTokenizer(String vocabPath) {
        loadVocab(vocabPath);
    }

    private void loadVocab(String vocabPath) {
        try {
            InputStream is = new ClassPathResource(vocabPath).getInputStream();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
                String line;
                long index = 0;
                while ((line = reader.readLine()) != null) {
                    String token = line.trim();
                    if (!token.isEmpty()) {
                        vocab.put(token, index);
                        if (CLS_TOKEN.equals(token)) clsId = index;
                        else if (SEP_TOKEN.equals(token)) sepId = index;
                        else if (UNK_TOKEN.equals(token)) unkId = index;
                        else if (PAD_TOKEN.equals(token)) padId = index;
                    }
                    index++;
                }
            }
            log.info("Nạp thành công từ điển Bert WordPiece Vocabulary: {} tokens", vocab.size());
        } catch (Exception e) {
            log.error("Không thể nạp file từ điển vocabulary từ [{}]: {}", vocabPath, e.getMessage());
        }
    }

    public static String removeDiacritics(String text) {
        if (text == null) return "";
        String normalized = Normalizer.normalize(text, Normalizer.Form.NFD);
        String withoutDiacritics = DIACRITICS_PATTERN.matcher(normalized).replaceAll("");
        return withoutDiacritics.replace('đ', 'd').replace('Đ', 'D');
    }

    public TokenizedInput tokenize(String text, int maxSeqLength) {
        if (text == null) text = "";
        
        // 1. Chuẩn hóa tiếng Việt và chuyển chữ thường
        String cleanText = removeDiacritics(text).toLowerCase();

        // 2. Tách từ theo khoảng trắng và dấu câu
        String[] words = cleanText.split("[\\s\\p{Punct}]+");

        List<Long> tokenIds = new ArrayList<>();
        tokenIds.add(clsId);

        for (String word : words) {
            if (word.isEmpty()) continue;

            if (vocab.containsKey(word)) {
                tokenIds.add(vocab.get(word));
            } else {
                // WordPiece subword splitting
                int start = 0;
                while (start < word.length()) {
                    int end = word.length();
                    boolean found = false;
                    while (start < end) {
                        String sub = (start == 0 ? "" : "##") + word.substring(start, end);
                        if (vocab.containsKey(sub)) {
                            tokenIds.add(vocab.get(sub));
                            start = end;
                            found = true;
                            break;
                        }
                        end--;
                    }
                    if (!found) {
                        tokenIds.add(unkId);
                        start++;
                    }
                }
            }

            if (tokenIds.size() >= maxSeqLength - 1) {
                break;
            }
        }

        tokenIds.add(sepId);

        int seqLen = Math.min(tokenIds.size(), maxSeqLength);
        long[][] inputIds = new long[1][maxSeqLength];
        long[][] attentionMask = new long[1][maxSeqLength];
        long[][] tokenTypeIds = new long[1][maxSeqLength];

        for (int i = 0; i < maxSeqLength; i++) {
            if (i < seqLen) {
                inputIds[0][i] = tokenIds.get(i);
                attentionMask[0][i] = 1L;
            } else {
                inputIds[0][i] = padId;
                attentionMask[0][i] = 0L;
            }
            tokenTypeIds[0][i] = 0L;
        }

        return new TokenizedInput(inputIds, attentionMask, tokenTypeIds, maxSeqLength);
    }
}
