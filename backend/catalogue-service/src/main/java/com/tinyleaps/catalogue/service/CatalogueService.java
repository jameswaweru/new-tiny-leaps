package com.tinyleaps.catalogue.service;

import com.tinyleaps.catalogue.dto.GameView;
import com.tinyleaps.catalogue.entity.VirtualGame;
import com.tinyleaps.catalogue.repository.VirtualGameRepository;
import com.tinyleaps.common.api.ApiException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CatalogueService {

    private final VirtualGameRepository repository;

    public CatalogueService(VirtualGameRepository repository) {
        this.repository = repository;
    }

    public Page<GameView> list(String category, Integer age, int page, int size) {
        String cat = category == null || category.isBlank() ? null : category.trim().toUpperCase();
        return repository.search(cat, age, PageRequest.of(page, size)).map(CatalogueService::toView);
    }

    public GameView get(String gameId) {
        return repository.findByGameIdAndStatus(gameId, "ACTIVE").map(CatalogueService::toView)
                .orElseThrow(() -> ApiException.notFound("GAME_NOT_FOUND: " + gameId));
    }

    public List<String> categories() {
        return repository.activeCategories();
    }

    static GameView toView(VirtualGame g) {
        return new GameView(g.getGameId(), g.getTitle(), g.getDescription(), g.getCategory(), g.getMinAge(),
                g.getMaxAge(), g.getMinAge() + "-" + g.getMaxAge() + " yrs", split(g.getThemes()), split(g.getSkills()),
                g.getThumbnailPath(), "/games/" + g.getGameId(), g.getThemeColor());
    }

    private static List<String> split(String csv) {
        return csv == null || csv.isBlank() ? List.of()
                : Arrays.stream(csv.split(",")).map(String::trim).filter(s -> !s.isEmpty()).toList();
    }
}
