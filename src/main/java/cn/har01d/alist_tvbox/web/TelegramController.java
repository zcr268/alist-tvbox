package cn.har01d.alist_tvbox.web;

import cn.har01d.alist_tvbox.dto.tg.Message;
import cn.har01d.alist_tvbox.dto.tg.SearchRequest;
import cn.har01d.alist_tvbox.entity.TelegramChannel;
import cn.har01d.alist_tvbox.entity.TelegramChannelRepository;
import cn.har01d.alist_tvbox.service.SubscriptionService;
import cn.har01d.alist_tvbox.service.TelegramService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import telegram4j.tl.User;

import java.io.IOException;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
public class TelegramController {
    private final TelegramChannelRepository telegramChannelRepository;
    private final TelegramService telegramService;
    private final SubscriptionService subscriptionService;
    private final ObjectMapper objectMapper;

    public TelegramController(TelegramChannelRepository telegramChannelRepository,
                              TelegramService telegramService,
                              SubscriptionService subscriptionService,
                              ObjectMapper objectMapper) {
        this.telegramChannelRepository = telegramChannelRepository;
        this.telegramService = telegramService;
        this.subscriptionService = subscriptionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/api/telegram/reset")
    public void reset() {
        telegramService.reset();
    }

    @PostMapping("/api/telegram/login")
    public void login() {
        telegramService.connect();
    }

    @PostMapping("/api/telegram/logout")
    public void logout() {
        telegramService.logout();
    }

    @GetMapping("/api/telegram/search")
    public List<Message> searchByKeyword(String wd) {
        return telegramService.search(wd, 100, false, false);
    }

    @GetMapping("/tg-search")
    public Object browse(String id, String t, String wd, boolean web, @RequestParam(required = false, defaultValue = "1") int pg) throws IOException {
        return browse("", id, t, wd, web, pg);
    }

    @GetMapping("/tg-search/{token}")
    public Object browse(@PathVariable String token, String id, String t, String wd, boolean web, @RequestParam(required = false, defaultValue = "1") int pg) throws IOException {
        subscriptionService.checkToken(token);
        if (StringUtils.isNotBlank(id)) {
            return telegramService.detail(id);
        } else if (StringUtils.isNotBlank(t)) {
            if (t.equals("0")) {
                return telegramService.searchMovies("", web, 5);
            }
            return telegramService.list(t, web, pg);
        } else if (StringUtils.isNotBlank(wd)) {
            return telegramService.searchMovies(wd, web, 20);
        }
        return telegramService.category(web);
    }

    @GetMapping("/tg-db")
    public Object db(String id, String t, String wd, String sort, Integer year, String genre, String region, @RequestParam(required = false, defaultValue = "1") int pg) throws IOException {
        return db("", id, t, wd, sort, year, genre, region, pg);
    }

    @GetMapping("/tg-db/{token}")
    public Object db(@PathVariable String token, String id, String t, String wd, String sort, Integer year, String genre, String region, @RequestParam(required = false, defaultValue = "1") int pg) throws IOException {
        subscriptionService.checkToken(token);
        if (StringUtils.isNotBlank(id)) {
            return telegramService.detail(id);
        } else if (StringUtils.isNotBlank(t)) {
            if (t.equals("0")) {
                t = "suggestion";
            }
            return telegramService.listDouban(t, sort, year, genre, region, pg);
        } else if (StringUtils.isNotBlank(wd)) {
            return telegramService.searchDouban(wd, 20);
        }
        return telegramService.categoryDouban();
    }

    @GetMapping("/tgsz")
    public Map<String, Object> searchZx(String keyword, String channelUsername, HttpServletResponse response) {
        response.setHeader("server", "hypercorn-h11");
        return telegramService.searchZx(keyword, channelUsername);
    }

    @GetMapping("/tgs")
    public String searchPg(String keyword, String channelUsername, String encode, HttpServletResponse response) {
        response.setHeader("server", "hypercorn-h11");
        return telegramService.searchPg(keyword, channelUsername, encode);
    }

    @PostMapping("/tgs")
    public String searchPgPost(@RequestBody String body, HttpServletResponse response) throws JsonProcessingException {
        String json = new String(Base64.getDecoder().decode(body));
        log.debug("searchPgPost: {} {}", body, json);
        SearchRequest request = objectMapper.readValue(json, SearchRequest.class);
        response.setHeader("server", "hypercorn-h11");
        if ("2".equals(request.getPage())) {
            return "";
        }
        return telegramService.searchPg(request.getKeyword(), request.getChannelUsername(), request.getEncode());
    }

    @GetMapping(value = "/tgs/s/{id}", produces = "text/plain;charset=UTF-8")
    public String searchWeb(@PathVariable String id, String keyword, String encode, HttpServletResponse response) {
        response.setHeader("server", "hypercorn-h11");
        return telegramService.searchWeb(keyword, id, encode);
    }

    @PostMapping(value = "/tgs/s/{id}", produces = "text/plain;charset=UTF-8")
    public String searchWebPost(@PathVariable String id, @RequestBody String body, HttpServletResponse response) throws JsonProcessingException {
        String json = new String(Base64.getDecoder().decode(body));
        SearchRequest request = objectMapper.readValue(json, SearchRequest.class);
        response.setHeader("server", "hypercorn-h11");
        if ("2".equals(request.getPage())) {
            return "";
        }
        return telegramService.searchWeb(request.getKeyword(), request.getChannelUsername(), request.getEncode());
    }

    @GetMapping("/api/telegram/user")
    public User getUser() {
        return telegramService.getUser();
    }

    @GetMapping("/api/telegram/chats")
    public List<TelegramChannel> getAllChats() {
        return telegramService.getAllChats();
    }

    @GetMapping("/api/telegram/channels")
    public List<TelegramChannel> list() {
        return telegramService.list();
    }

    @PostMapping("/api/telegram/resolveUsername")
    public TelegramChannel create(@RequestBody TelegramChannel channel) {
        return telegramService.create(channel);
    }

    @PostMapping("/api/telegram/channels")
    public TelegramChannel save(@RequestBody TelegramChannel channel) {
        return telegramChannelRepository.save(channel);
    }

    @PutMapping("/api/telegram/channels")
    public List<TelegramChannel> updateAll(@RequestBody List<TelegramChannel> channels) {
        return telegramService.updateAll(channels);
    }

    @DeleteMapping("/api/telegram/channels/{id}")
    public void delete(@PathVariable Long id) {
        telegramChannelRepository.deleteById(id);
    }

    @PostMapping("/api/telegram/reloadChannels")
    public List<TelegramChannel> reloadChannels() throws IOException {
        return telegramService.reloadChannels();
    }

    @PostMapping("/api/telegram/validateChannels")
    public List<TelegramChannel> validateChannels() {
        return telegramService.validateChannels();
    }

    @GetMapping("/api/telegram/history")
    public List<Message> getChatHistory(String id) {
        return telegramService.getHistory(id);
    }
}
