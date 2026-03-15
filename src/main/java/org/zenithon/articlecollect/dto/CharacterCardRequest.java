package org.zenithon.articlecollect.dto;

/**
 * 角色卡更新请求 DTO
 */
public class CharacterCardRequest {
    
    private String characterCards;
    
    public CharacterCardRequest() {
    }
    
    public CharacterCardRequest(String characterCards) {
        this.characterCards = characterCards;
    }
    
    public String getCharacterCards() {
        return characterCards;
    }
    
    public void setCharacterCards(String characterCards) {
        this.characterCards = characterCards;
    }
}
