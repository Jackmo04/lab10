package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {

    private static final String CONFIG_FILE = "config.yml";
    private final DrawNumber model;
    private final List<DrawNumberView> views;
    private final Configuration config;

    /**
     * @param views
     *              the views to attach
     */
    public DrawNumberApp(final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view : views) {
            view.setObserver(this);
            view.start();
        }
        this.config = this.loadConfiguration();
        this.model = new DrawNumberImpl(
                config.getMin(),
                config.getMax(),
                config.getAttempts());
    }

    private Configuration loadConfiguration() {
        try (
            final BufferedReader reader = new BufferedReader(
                new InputStreamReader(ClassLoader.getSystemResourceAsStream(CONFIG_FILE))
            );
        ) {
            Configuration.Builder confBuilder = new Configuration.Builder();
            String line;
            while ((line = reader.readLine()) != null) {
                var option = line.split(":\s");
                switch (option[0]) {
                    case "minimum":
                        confBuilder.setMin(Integer.parseInt(option[1]));
                        break;
                    case "maximum":
                        confBuilder.setMax(Integer.parseInt(option[1]));
                        break;
                    case "attempts":
                        confBuilder.setAttempts(Integer.parseInt(option[1]));
                        break;
                    default:
                        throw new IllegalArgumentException("Option " + option[0] + "doesn't exist");
                }
            }
            final Configuration conf = confBuilder.build();
            if (!conf.isConsistent()) {
                for (final DrawNumberView view : this.views) {
                    view.displayError("Configuration is inconsistent");
                    quit();
                }
            }
            return conf;

        } catch (IOException e) {
            for (final DrawNumberView view : this.views) {
                view.displayError("Failed to load configuration file");
                quit();
            }
            return null;
        }

    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view : views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view : views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *             ignored
     * @throws FileNotFoundException
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp(new DrawNumberViewImpl());
    }

}
