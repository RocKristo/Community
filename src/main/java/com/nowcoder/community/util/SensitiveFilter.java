package com.nowcoder.community.util;

import jakarta.annotation.PostConstruct;
import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

@Component
public class SensitiveFilter {
    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);
    private TrieNode rootNode = new TrieNode();
    private static final String REPLACEMENT = "***";

    @PostConstruct
    public void init() {
        try (InputStream is = this.getClass().getClassLoader().getResourceAsStream("sensitive-words.txt");
             BufferedReader reader = new BufferedReader(new InputStreamReader(is))) {
            String keyword;
            while ((keyword = reader.readLine()) != null) {
                this.addKeyword(keyword);
            }
        } catch (IOException e) {
            logger.error("敏感词加载失败" + e.getMessage());
        }
    }

    public void addKeyword(String keyword) {
        TrieNode tempNode = rootNode;
        for (int i = 0; i < keyword.length(); i++) {
            char c = keyword.charAt(i);
            TrieNode subNode = tempNode.getSubNode(c);
            if (subNode == null) {
                subNode = new TrieNode();
                tempNode.addSubNode(c, subNode);
            }
            tempNode = subNode;
            if (i == keyword.length() - 1) {
                subNode.setKeyWordEnd(true);
            }
        }

    }
    public boolean isSymbol(Character c){
        return !CharUtils.isAsciiAlphanumeric(c) && (c<0x2E80||c>0x9FFF);
    }

    public String filter(String text) {
        if (StringUtils.isBlank(text)) {
            return null;
        }
        TrieNode tempNode = rootNode;
        int begin = 0;
        int position = 0;
        StringBuilder sb = new StringBuilder();
        while(begin<text.length()){
            if(position < text.length()) {
                Character c = text.charAt(position);

                // 跳过符号
                if (isSymbol(c)) {
                    if (tempNode == rootNode) {
                        begin++;
                        sb.append(c);
                    }
                    position++;
                    continue;
                }

                // 检查下级节点
                tempNode = tempNode.getSubNode(c);
                if (tempNode == null) {
                    // 以begin开头的字符串不是敏感词
                    sb.append(text.charAt(begin));
                    // 进入下一个位置
                    position = ++begin;
                    // 重新指向根节点
                    tempNode = rootNode;
                }
                // 发现敏感词
                else if (tempNode.isKeywordEnd()) {
                    sb.append(REPLACEMENT);
                    begin = ++position;
                }
                // 检查下一个字符
                else {
                    position++;
                }
            }
            // position遍历越界仍未匹配到敏感词
            else{
                sb.append(text.charAt(begin));
                position = ++begin;
                tempNode = rootNode;
            }
        }
        return sb.toString();
    }

    private class TrieNode {
        private boolean isKeyWordEnd = false;

        private Map<Character, TrieNode> subnodes = new HashMap<>();

        public void addSubNode(Character c, TrieNode node) {
            subnodes.put(c, node);
        }

        public TrieNode getSubNode(Character c) {
            return subnodes.get(c);
        }

        public boolean isKeywordEnd() {
            return isKeyWordEnd;
        }

        public void setKeyWordEnd(boolean keyWordEnd) {
            isKeyWordEnd = keyWordEnd;
        }
    }
}
