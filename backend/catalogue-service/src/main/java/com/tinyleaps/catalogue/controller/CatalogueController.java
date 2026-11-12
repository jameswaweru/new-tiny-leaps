package com.tinyleaps.catalogue.controller;

import com.tinyleaps.catalogue.dto.GameView;
import com.tinyleaps.catalogue.service.CatalogueService;
import com.tinyleaps.common.api.ApiResponse;
import com.tinyleaps.common.api.Pagination;
import com.tinyleaps.common.logging.TLLog;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/catalogue")
public class CatalogueController {

    private final CatalogueService service;

    public CatalogueController(CatalogueService service) {
        this.service = service;
    }

    @GetMapping("/games")
    public ApiResponse<List<GameView>> games(@RequestParam(required = false) String category,
                                             @RequestParam(required = false) Integer age,
                                             @RequestParam(defaultValue = "0") int page,
                                             @RequestParam(defaultValue = "20") int size) {
        TLLog.process("LIST_GAMES");
        Page<GameView> p = service.list(category, age, Math.max(page, 0), Math.min(Math.max(size, 1), 50));
        return ApiResponse.page(p.isEmpty() ? "No games here yet - check back soon!" : "Let's play!",
                "Games catalogue", p.getContent(), Pagination.of(p.getNumber(), p.getSize(), p.getTotalElements()));
    }

    @GetMapping("/games/{gameId}")
    public ApiResponse<GameView> game(@PathVariable String gameId) {
        TLLog.process("GET_GAME");
        return ApiResponse.ok("Let's play!", "Game details", service.get(gameId));
    }

    @GetMapping("/categories")
    public ApiResponse<List<String>> categories() {
        TLLog.process("LIST_CATEGORIES");
        return ApiResponse.ok("Categories loaded", "Active game categories", service.categories());
    }
}
