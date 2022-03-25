package com.nowcoder.community.util;

import org.apache.commons.lang3.CharUtils;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.imageio.IIOException;
import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class SensitiveFilter {

    private static final Logger logger = LoggerFactory.getLogger(SensitiveFilter.class);

    private class TrieNode {

        private boolean isEnd = false;

        private Map<Character, TrieNode> child;

        TrieNode() {
            child = new HashMap<Character, TrieNode>();
        }
    }


    TrieNode root = new TrieNode();

    SensitiveFilter(@Value("${community.sensitive-words.path}") String sensitiveWordsPath) {

        List<String> wordList = new ArrayList<>();

        try {
            ClassPathResource classPathResource = new ClassPathResource(sensitiveWordsPath);
            FileReader fileReader = new FileReader(classPathResource.getFile());
            BufferedReader bufferedReader = new BufferedReader(fileReader);

            String line;
            while ((line = bufferedReader.readLine()) != null) {
                line = StringUtils.strip(line);
                wordList.add(line);
            }
        } catch (FileNotFoundException e) {
            logger.error("敏感词文件不存在！：" + e.getMessage());
        } catch (Exception e) {
            logger.error("敏感词文件读取错误！：" + e.getMessage());
        }

        for (String word : wordList) {
            insertWord(word);
        }
    }

    public void insertWord(String word) {
        TrieNode p = root;
        for (char c : word.toCharArray()) {
            if(!p.child.containsKey(c)) {
                p.child.put(c, new TrieNode());
            }
            p = p.child.get(c);
        }
        p.isEnd = true;
    }

    public String filter(String text) {

        StringBuffer sb = new StringBuffer();
        TrieNode p = root;
        int n = text.length();
        int i = 0, j = 0;

        while (i < n) {
            j = i;
            boolean getOne = false;
            while (j < n) {
                char c = text.charAt(j);
                if (!isSymbol(c)) {
                    p = p.child.get(c);
                    if (p == null) {
                        sb.append(text.charAt(i++));
                        break;
                    }
                    if (p.isEnd) {
                        while (i <= j){
                            sb.append("*");
                            i++;
                        }
                        getOne = true;
                    }
                    else if (getOne) {
                        break;
                    }
                }
                else if(j == i && !getOne) {
                    sb.append(text.charAt(i++));
                    break;
                }
                j++;
            }

            if(j == n) {
                while(i < j) {
                    sb.append(text.charAt(i++));
                }
            }
            p = root;
        }

        return sb.toString();
    }

    // 判断是否是符号
    private boolean isSymbol(Character c) {
        // 非数字字母且不是汉字
        return (c < 0x2E80 || c > 0x9FFF) && !CharUtils.isAsciiAlphanumeric(c);
    }

}
