package com.github.yannicklamprecht.mc.management.console.web;

import com.github.yannicklamprecht.mc.management.console.config.ConsoleUiProperties;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Controller
public class DashboardController {

    private final DashboardEventBroadcaster eventBroadcaster;
    private final ConsoleUiProperties uiProperties;

    public DashboardController(DashboardEventBroadcaster eventBroadcaster, ConsoleUiProperties uiProperties) {
        this.eventBroadcaster = eventBroadcaster;
        this.uiProperties = uiProperties;
    }

    @GetMapping("/")
    public String dashboard(Model model) {
        // The page shell is otherwise static; the server list, their status/players/etc. and the
        // version badges are all populated client-side from /api/servers and /api/servers/{id}/*,
        // since which servers exist is runtime configuration (minecraft.management.servers.*),
        // not something to bake into the server-rendered template. These few flags are the only UI
        // configuration the frontend needs before it can render, so they're handed over here.
        model.addAttribute("hideOfflineServers", uiProperties.isHideOfflineServers());
        model.addAttribute("showNotifyOperatorsCheckbox", uiProperties.getOperatorNotifications().showsCheckbox());
        model.addAttribute("notifyOperatorsDefaultChecked", uiProperties.getOperatorNotifications().defaultChecked());
        return "dashboard";
    }

    @GetMapping("/api/events")
    public SseEmitter events() {
        return eventBroadcaster.subscribe();
    }
}
