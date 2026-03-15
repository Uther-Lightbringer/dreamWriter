package org.zenithon.articlecollect.dto;

import java.util.List;

/**
 * 角色卡批量更新请求 DTO
 */
public class CharacterCardBatchRequest {
    
    private List<CharacterCard> characterCards;
    
    public CharacterCardBatchRequest() {
    }
    
    public CharacterCardBatchRequest(List<CharacterCard> characterCards) {
        this.characterCards = characterCards;
    }
    
    public List<CharacterCard> getCharacterCards() {
        return characterCards;
    }
    
    public void setCharacterCards(List<CharacterCard> characterCards) {
        this.characterCards = characterCards;
    }
}
