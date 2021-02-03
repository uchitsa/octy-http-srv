package io.github.uchitsa;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousServerSocketChannel;
import java.nio.channels.AsynchronousSocketChannel;
import java.util.Arrays;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

public class Main {

    public static void main(String[] args) {

        new Server().bootstrap();
    }
}


class Server {
    private static final int BUFFER_SIZE = 256;
    public AsynchronousServerSocketChannel server;
    private static final String HEADERS = "HTTP/1.1 200 ok \n" +
            "Server: octy\n" +
            "Content-type: text/html\n" +
            "Content-length: %s\n" +
            "Connection: close\n\n";

    public void bootstrap() {
        try {
            server = AsynchronousServerSocketChannel.open();
            server.bind(new InetSocketAddress("127.0.0.1", 8088));

            while (true) {
                Future<AsynchronousSocketChannel> future = server.accept();
                handleClient(future);
            }

        } catch (IOException | InterruptedException | ExecutionException | TimeoutException e) {
            e.printStackTrace();
        }
    }

    private void handleClient(Future<AsynchronousSocketChannel> future) throws InterruptedException, ExecutionException, TimeoutException, IOException {
        System.out.println("New client thread");

        AsynchronousSocketChannel clientChannel = future.get(30, TimeUnit.SECONDS);

        while (clientChannel != null && clientChannel.isOpen()) {
            ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
            StringBuilder sb = new StringBuilder();
            boolean isReading = true;

            while (isReading) {
                clientChannel.read(buffer).get();

                int pos = buffer.position();
                isReading = pos == BUFFER_SIZE;

                byte[] array = isReading ?
                        buffer.array()
                        : Arrays.copyOfRange(buffer.array(), 0, pos);

                sb.append(new String(array));
                System.out.println(sb);
                buffer.clear();
            }

            String body = "<html><body><h1>Hello, octy</h1></body></html>";
            String page = String.format(HEADERS, +body.length()) + body;
            ByteBuffer resp = ByteBuffer.wrap(page.getBytes());

            clientChannel.write(resp);
            clientChannel.close();
        }
    }
}