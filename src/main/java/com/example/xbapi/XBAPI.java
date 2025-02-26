package com.example.xbapi;

import net.milkbowl.vault.economy.Economy;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.time.LocalDate;
import java.util.*;

public class XBAPI extends JavaPlugin {

    // ================ 插件元信息 ================
    private static final String[] LOGO = {
            "╔═══╗╔╗  ╔═══╗╔═══╗╔╗╔═╗",
            "║╔═╗║║║  ║╔══╝║╔═╗║║║║╔╝",
            "║║ ║║║║  ║╚══╗║║ ║║║╚╝╝ ",
            "║╚═╝║║║ ╔╣╔══╝║║ ║║║╔╗║ ",
            "║╔═╗║║╚═╝║╚══╗║╚═╝║║║║╚╗",
            "╚╝ ╚╝╚═══╩═══╝╚═══╝╚╝╚═╝"
    };

    // ================ 核心组件 ================
    private Economy economy; // Vault 经济系统
    private Server server; // Jetty HTTP 服务器
    private boolean debugMode = false; // Debug 模式开关
    private final Map<String, LocalDate> signRecords = new HashMap<>(); // 签到记录 <玩家名, 最后签到日期>

    // ================ 配置管理 ================
    private Config config;
    private Lang lang;

    @Override
    public void onEnable() {
        // 显示启动 LOGO
        printLogo(ChatColor.AQUA, "启 动");

        // 初始化配置
        config = new Config(this);
        lang = new Lang(this);
        debugMode = config.isDebugEnabled();

        // 初始化经济系统
        if (!setupEconomy()) {
            getLogger().warning(lang.get("economy.disabled"));
        }

        // 启动 HTTP 服务器
        startHTTPServer();
        getLogger().info(ChatColor.GREEN + "✅ 插件已就绪");
    }

    @Override
    public void onDisable() {
        // 显示关闭 LOGO
        printLogo(ChatColor.RED, "关 闭");
        stopHTTPServer();
        getLogger().info(ChatColor.GOLD + "⛔ 插件已卸载");
    }

    // ================ 经济系统初始化 ================
    private boolean setupEconomy() {
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp != null) economy = rsp.getProvider();
        return economy != null;
    }

    // ================ HTTP 服务器控制 ================
    private void startHTTPServer() {
        server = new Server(config.getPort());
        ServletContextHandler context = new ServletContextHandler();
        context.setContextPath("/");

        // 注册 API 端点
        context.addServlet(new ServletHolder(new SignServlet()), config.getSignPath());
        context.addServlet(new ServletHolder(new PlayersServlet()), config.getPlayersPath());
        context.addServlet(new ServletHolder(new EconomyServlet()), config.getEconomyPath());
        context.addServlet(new ServletHolder(new BalanceServlet()), config.getBalancePath());

        server.setHandler(context);
        try {
            server.start();
            getLogger().info(ChatColor.GREEN + "🌐 监听端口: " + config.getPort());
        } catch (Exception e) {
            getLogger().severe("❌ HTTP 服务器启动失败: " + e.getMessage());
        }
    }

    private void stopHTTPServer() {
        if (server != null) {
            try {
                server.stop();
                getLogger().info(ChatColor.YELLOW + "🔌 已关闭 HTTP 服务器");
            } catch (Exception e) {
                getLogger().severe("❌ 关闭 HTTP 服务器时发生错误: " + e.getMessage());
            }
        }
    }

    // ================ 控制台美化输出 ================
    private void printLogo(ChatColor color, String status) {
        Arrays.stream(LOGO).forEach(line -> getLogger().info(color + line));
        getLogger().info(color + "➤➤➤ " + status);
    }

    // ================ 指令处理 ================
    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (cmd.getName().equalsIgnoreCase("xbapi")) {
            if (args.length == 0) {
                sender.sendMessage(ChatColor.RED + "用法: /xbapi <reload|debug|resetsign>");
                return true;
            }

            switch (args[0].toLowerCase()) {
                case "reload":
                    config.reload();
                    lang.reload();
                    debugMode = config.isDebugEnabled();
                    sender.sendMessage(ChatColor.GREEN + "配置已重载！");
                    return true;

                case "debug":
                    debugMode = !debugMode;
                    config.setDebugEnabled(debugMode);
                    sender.sendMessage("Debug 模式已 " + (debugMode ? "启用" : "禁用"));
                    return true;

                case "resetsign":
                    if (args.length < 2) {
                        sender.sendMessage(ChatColor.RED + "用法: /xbapi resetsign <玩家名>");
                        return true;
                    }
                    signRecords.remove(args[1]);
                    sender.sendMessage(ChatColor.GREEN + "已重置玩家 " + args[1] + " 的签到状态！");
                    return true;

                default:
                    sender.sendMessage(ChatColor.RED + "未知指令！");
                    return true;
            }
        }
        return false;
    }

    // ================ Servlet 实现 ================
    private class SignServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            handleSignRequest(req, resp);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            handleSignRequest(req, resp);
        }

        private void handleSignRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String playerName = req.getParameter("player");
            if (playerName == null || playerName.isEmpty()) {
                resp.sendError(400, "参数 player 不能为空");
                return;
            }

            // 检查玩家是否已签到
            LocalDate lastSign = signRecords.get(playerName);
            if (lastSign != null && lastSign.equals(LocalDate.now())) {
                resp.getWriter().write("{\"status\":\"already_signed\", \"message\":\"今日已签到\"}");
                return;
            }

            // 发放签到奖励
            double reward = config.getSignReward();
            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            if (economy != null) {
                economy.depositPlayer(player, reward);
            }

            // 记录签到
            signRecords.put(playerName, LocalDate.now());
            resp.getWriter().write("{\"status\":\"success\", \"reward\":" + reward + "}");
        }
    }

    private class PlayersServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            List<String> players = new ArrayList<>();
            for (Player player : Bukkit.getOnlinePlayers()) {
                players.add(player.getName());
            }
            resp.setContentType("application/json");
            resp.getWriter().write("{\"players\":" + players + "}");
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            doGet(req, resp);
        }
    }

    private class EconomyServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            handleEconomyRequest(req, resp);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            handleEconomyRequest(req, resp);
        }

        private void handleEconomyRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String action = req.getParameter("action");
            String playerName = req.getParameter("player");
            double amount = Double.parseDouble(req.getParameter("amount"));

            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            boolean success = false;

            switch (action.toLowerCase()) {
                case "give":
                    success = economy.depositPlayer(player, amount).transactionSuccess();
                    break;
                case "take":
                    success = economy.withdrawPlayer(player, amount).transactionSuccess();
                    break;
                case "set":
                    economy.withdrawPlayer(player, economy.getBalance(player));
                    success = economy.depositPlayer(player, amount).transactionSuccess();
                    break;
                case "reset":
                    economy.withdrawPlayer(player, economy.getBalance(player));
                    success = true;
                    break;
            }

            resp.getWriter().write("{\"status\":" + success + "}");
        }
    }

    private class BalanceServlet extends HttpServlet {
        @Override
        protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            handleBalanceRequest(req, resp);
        }

        @Override
        protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            handleBalanceRequest(req, resp);
        }

        private void handleBalanceRequest(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            String playerName = req.getParameter("player");
            if (playerName == null || playerName.isEmpty()) {
                resp.sendError(400, "参数 player 不能为空");
                return;
            }

            OfflinePlayer player = Bukkit.getOfflinePlayer(playerName);
            if (player == null || !player.hasPlayedBefore()) {
                resp.getWriter().write("{\"status\":\"error\", \"message\":\"玩家未找到\"}");
                return;
            }

            double balance = economy.getBalance(player);
            resp.getWriter().write("{\"status\":\"success\", \"player\":\"" + playerName + "\", \"balance\":" + balance + "}");
        }
    }

    // ================ 配置管理类 ================
    private static class Config {
        private final XBAPI plugin;
        private YamlConfiguration config;

        Config(XBAPI plugin) {
            this.plugin = plugin;
            reload();
        }

        void reload() {
            File file = new File(plugin.getDataFolder(), "config.yml");
            if (!file.exists()) plugin.saveResource("config.yml", false);
            config = YamlConfiguration.loadConfiguration(file);
        }

        int getPort() { return config.getInt("port", 8080); }
        String getApiPath() { return config.getString("api-path", "/xbapi"); }
        String getSignPath() { return config.getString("sign-path", getApiPath() + "/sign"); }
        String getPlayersPath() { return config.getString("players-path", getApiPath() + "/players"); }
        String getEconomyPath() { return config.getString("economy-path", getApiPath() + "/economy"); }
        String getBalancePath() { return config.getString("balance-path", getApiPath() + "/balance"); }
        boolean isDebugEnabled() { return config.getBoolean("debug", false); }
        void setDebugEnabled(boolean enabled) { config.set("debug", enabled); }
        double getSignReward() { return config.getDouble("sign-reward", 1000.0); }
        boolean allowAllIPs() { return config.getBoolean("security.allow-all-ips", false); }
        List<String> getAllowedIPs() { return config.getStringList("security.allowed-ips"); }
    }

    private static class Lang {
        private final XBAPI plugin;
        private YamlConfiguration lang;

        Lang(XBAPI plugin) {
            this.plugin = plugin;
            reload();
        }

        void reload() {
            File file = new File(plugin.getDataFolder(), "lang/" + plugin.getConfig().getString("language", "zh_cn") + ".yml");
            if (!file.exists()) plugin.saveResource("lang/zh_cn.yml", false);
            lang = YamlConfiguration.loadConfiguration(file);
        }

        String get(String key) {
            return lang.getString(key, "Missing translation: " + key);
        }
    }
}