package com.tinyleaps.games.painting;

import com.tinyleaps.common.logging.TLLog;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Serves the game entry page at {@code /games/{gameId}}. Only reachable through the gateway, which has
 * already validated the user's token with auth-service. The page itself is never cached so every launch
 * is re-validated; its assets under {@code /assets} are cached by the WebView.
 */
@RestController
public class GameEntryController {

    private final Resource index;

    public GameEntryController(@Value("${tinyleaps.game.id}") String gameId) {
        this.index = new ClassPathResource("static/games/" + gameId + "/index.html");
    }

    @GetMapping({"/games/${tinyleaps.game.id}", "/games/${tinyleaps.game.id}/"})
    public ResponseEntity<Resource> entry() {
        TLLog.process("GAME_ENTRY");
        return ResponseEntity.ok()
                .contentType(MediaType.TEXT_HTML)
                .cacheControl(CacheControl.noStore())
                .header("Content-Security-Policy", "default-src 'self'; img-src 'self' data: blob:; "
                        + "style-src 'self' 'unsafe-inline'; script-src 'self' 'unsafe-inline'; connect-src 'self'; frame-ancestors 'none'")
                .body(index);
    }
}
