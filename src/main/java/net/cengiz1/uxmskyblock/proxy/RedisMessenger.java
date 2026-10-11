package net.cengiz1.uxmskyblock.proxy;

import redis.clients.jedis.DefaultJedisClientConfig;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisClientConfig;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPubSub;

import java.util.function.Consumer;
import java.util.logging.Logger;

public class RedisMessenger {

    private final Logger logger;
    private final String channel;
    private final JedisPool pool;
    private final Consumer<String> messageHandler;

    private volatile boolean running = true;
    private JedisPubSub subscriber;
    private Thread subscriberThread;

    public RedisMessenger(Logger logger, String host, int port, String username, String password,
                          int timeoutSeconds, String channel, Consumer<String> messageHandler) {
        this.logger = logger;
        this.channel = channel;
        this.messageHandler = messageHandler;

        DefaultJedisClientConfig.Builder builder = DefaultJedisClientConfig.builder()
                .timeoutMillis(Math.max(1, timeoutSeconds) * 1000);
        if (username != null && !username.isEmpty())
            builder.user(username);
        if (password != null && !password.isEmpty())
            builder.password(password);
        JedisClientConfig clientConfig = builder.build();

        this.pool = new JedisPool(new HostAndPort(host, port), clientConfig);

        try (Jedis jedis = this.pool.getResource()) {
            jedis.ping();
        }

        startSubscriber();
    }

    private void startSubscriber() {
        this.subscriber = new JedisPubSub() {
            @Override
            public void onMessage(String channel, String message) {
                try {
                    messageHandler.accept(message);
                } catch (Throwable error) {
                    logger.warning("Could not handle proxy message: " + error.getMessage());
                }
            }
        };

        this.subscriberThread = new Thread(() -> {
            while (running) {
                try (Jedis jedis = pool.getResource()) {
                    jedis.subscribe(subscriber, channel);
                } catch (Throwable error) {
                    if (!running)
                        break;
                    logger.warning("Redis subscriber connection lost, retrying in 5s: " + error.getMessage());
                    sleep(5000L);
                }
            }
        }, "skyblock-proxy-redis-sub");
        this.subscriberThread.setDaemon(true);
        this.subscriberThread.start();
    }

    public void publish(String message) {
        try (Jedis jedis = pool.getResource()) {
            jedis.publish(channel, message);
        } catch (Throwable error) {
            logger.warning("Could not publish proxy message: " + error.getMessage());
        }
    }

    public void setWithExpiry(String key, String value, int seconds) {
        try (Jedis jedis = pool.getResource()) {
            jedis.setex(key, Math.max(1, seconds), value);
        } catch (Throwable error) {
            logger.warning("Redis setex failed (" + key + "): " + error.getMessage());
        }
    }

    public String takeValue(String key) {
        try (Jedis jedis = pool.getResource()) {
            return jedis.getDel(key);
        } catch (Throwable error) {
            logger.warning("Redis get/del failed (" + key + "): " + error.getMessage());
            return null;
        }
    }

    public void shutdown() {
        this.running = false;
        try {
            if (subscriber != null && subscriber.isSubscribed())
                subscriber.unsubscribe();
        } catch (Throwable ignored) {
        }
        try {
            if (pool != null)
                pool.close();
        } catch (Throwable ignored) {
        }
        if (subscriberThread != null)
            subscriberThread.interrupt();
    }

    private void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException error) {
            Thread.currentThread().interrupt();
        }
    }
}
