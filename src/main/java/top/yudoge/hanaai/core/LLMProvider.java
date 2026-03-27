package top.yudoge.hanaai.core;


/**
 * 是LLM对象的工厂，用于初始化LLM对象
 */
public interface LLMProvider {

    LLM create();

}
