package org.zenithon.articlecollect.service.genre;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.*;

/**
 * 体裁服务工厂
 * 根据体裁类型创建对应的服务实例
 */
@Service
public class GenreServiceFactory {

    private final Map<String, GenreService> genreServices = new HashMap<>();

    @Autowired
    public GenreServiceFactory(List<GenreService> services) {
        for (GenreService service : services) {
            genreServices.put(service.getGenreType(), service);
        }
    }

    /**
     * 获取体裁服务
     */
    public GenreService getGenreService(String genreType) {
        GenreService service = genreServices.get(genreType);
        if (service == null) {
            throw new RuntimeException("不支持的体裁类型: " + genreType);
        }
        return service;
    }

    /**
     * 获取所有可用体裁
     */
    public List<Map<String, Object>> getAvailableGenres() {
        List<Map<String, Object>> genres = new ArrayList<>();
        for (GenreService service : genreServices.values()) {
            Map<String, Object> genre = new HashMap<>();
            genre.put("type", service.getGenreType());
            genre.put("name", service.getGenreName());
            genres.add(genre);
        }
        return genres;
    }
}
