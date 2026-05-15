package com.farm.fpms.web;

import com.farm.fpms.domain.BusinessException;
import com.farm.fpms.service.BindToken;
import com.farm.fpms.service.BindTokenService;
import com.farm.fpms.service.MobileVisionService;
import com.farm.fpms.service.QrCodeService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

@Controller
public class MobileController {

    private final BindTokenService bindTokenService;
    private final MobileVisionService mobileVisionService;
    private final QrCodeService qrCodeService;
    private final JdbcTemplate jdbcTemplate;
    private final String qrBaseUrl;
    private final int tokenTtlSeconds;

    public MobileController(BindTokenService bindTokenService, MobileVisionService mobileVisionService,
                            QrCodeService qrCodeService, JdbcTemplate jdbcTemplate,
                            @Value("${fpms.ai.bind.qr-base-url:}") String qrBaseUrl,
                            @Value("${fpms.ai.bind.token-ttl-seconds:300}") int tokenTtlSeconds) {
        this.bindTokenService = bindTokenService;
        this.mobileVisionService = mobileVisionService;
        this.qrCodeService = qrCodeService;
        this.jdbcTemplate = jdbcTemplate;
        this.qrBaseUrl = qrBaseUrl;
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    @GetMapping("/mobile")
    public String mobileQr(HttpServletRequest request, HttpSession session, Model model) {
        String token = bindTokenService.issueToken(session.getId(), tokenTtlSeconds);
        String bindUrl = bindBaseUrl(request) + "?token=" + token;
        model.addAttribute("token", token);
        model.addAttribute("url", bindUrl);
        model.addAttribute("qrDataUri", qrCodeService.toDataUri(bindUrl));
        model.addAttribute("ttlSeconds", tokenTtlSeconds);
        return "mobile-qr";
    }

    @GetMapping("/m/bind")
    public String bind(@RequestParam String token, HttpServletRequest request, HttpSession session, Model model) {
        try {
            BindToken bindToken = bindTokenService.consume(token, request.getRemoteAddr(), request.getHeader("User-Agent"));
            session.setAttribute("MOBILE_BOUND", true);
            session.setAttribute("PC_SESSION_ID", bindToken.getPcSessionId());
            model.addAttribute("pcSessionId", bindToken.getPcSessionId());
            model.addAttribute("bound", true);
        } catch (BusinessException ex) {
            model.addAttribute("bound", false);
            model.addAttribute("error", ex.getMessage());
        }
        return "mobile-bind";
    }

    @PostMapping("/m/recognize")
    public String recognize(@RequestParam("image") MultipartFile image, HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute("MOBILE_BOUND"))) {
            model.addAttribute("bound", false);
            model.addAttribute("error", "手机端尚未绑定，请先从 PC 端扫码进入。");
            return "mobile-bind";
        }
        try {
            model.addAttribute("bound", true);
            model.addAttribute("pcSessionId", session.getAttribute("PC_SESSION_ID"));
            model.addAttribute("result", mobileVisionService.recognize(image));
        } catch (RuntimeException ex) {
            model.addAttribute("bound", true);
            model.addAttribute("pcSessionId", session.getAttribute("PC_SESSION_ID"));
            model.addAttribute("error", ex.getMessage());
        }
        return "mobile-bind";
    }

    @PostMapping("/m/add-crop")
    public String addCrop(@RequestParam String name, @RequestParam(required = false) String variety,
                          @RequestParam(required = false, defaultValue = "90") int minGrowthDays,
                          HttpSession session, Model model) {
        if (!Boolean.TRUE.equals(session.getAttribute("MOBILE_BOUND"))) {
            model.addAttribute("bound", false);
            model.addAttribute("error", "手机端尚未绑定");
            return "mobile-bind";
        }
        jdbcTemplate.update("insert into farm_crop(name, variety, min_growth_days) values(?,?,?)",
                name, variety, minGrowthDays);
        model.addAttribute("bound", true);
        model.addAttribute("pcSessionId", session.getAttribute("PC_SESSION_ID"));
        model.addAttribute("cropAdded", name);
        return "mobile-bind";
    }

    private String bindBaseUrl(HttpServletRequest request) {
        if (qrBaseUrl != null && !qrBaseUrl.trim().isEmpty() && !qrBaseUrl.contains("localhost")) {
            return qrBaseUrl.trim();
        }
        StringBuilder base = new StringBuilder();
        base.append(request.getScheme()).append("://").append(request.getServerName());
        int port = request.getServerPort();
        if (port != 80 && port != 443) {
            base.append(":").append(port);
        }
        base.append("/m/bind");
        return base.toString();
    }
}
