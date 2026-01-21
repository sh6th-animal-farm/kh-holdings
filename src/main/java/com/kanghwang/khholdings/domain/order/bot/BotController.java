package com.kanghwang.khholdings.domain.order.bot;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/bot")
public class BotController {

    @Autowired
    TradingSimulationBot tradingSimulationBot;

    @GetMapping("/start")
    public String start() {
        tradingSimulationBot.setRunning(true);
        return "============== 봇 주문 시작 ==============";
    }

    @GetMapping("/stop")
    public String stop() {
        tradingSimulationBot.setRunning(false);
        return "============== 봇 주문 중지 ==============";
    }

    @GetMapping("/status")
    public String status() {
        return tradingSimulationBot.isRunning() ? "작동 중" : "중지됨";
    }
}
