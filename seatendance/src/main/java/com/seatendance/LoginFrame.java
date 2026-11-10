package com.seatendance;

import javax.swing.*;

import org.mindrot.jbcrypt.BCrypt;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.net.URL;

public class LoginFrame extends ScaledFrame {

    // -------------------------------------------------------------------------
    // Colors
    // -------------------------------------------------------------------------

    static final Color maroon = new Color(128, 0, 0);
    static final Color maroonDark = new Color(90, 0, 0);
    static final Color maroonDeep = new Color(60, 0, 0);
    static final Color white = Color.WHITE;
    static final Color black = Color.BLACK;
    static final Color darkGray = new Color(30, 30, 30);
    static final Color darkGray2 = new Color(42, 42, 42);
    static final Color gray = new Color(60, 60, 60);
    static final Color lightGray = new Color(90, 90, 90);

    // -------------------------------------------------------------------------
    // Constructor
    // -------------------------------------------------------------------------

    public LoginFrame(MainSeatendanceApp seatendanceApp, double scale) {
        super(scale);

        // --- Size Calculation ---
        int basePanelWidth = 1000;
        int basePanelHeight = 700;

        int panelWidth = s(basePanelWidth);
        int panelHeight = s(basePanelHeight);

        int topBarHeight = s(45);

        // --- Frame Shape ---
        RoundRectangle2D frameShape = new RoundRectangle2D.Double(0, 0, panelWidth, panelHeight, 20, 20);

        // --- Frame Setup ---
        setUndecorated(true);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE); // the program does not end without this
        setBackground(new Color(0, 0, 0, 0)); // I made the frame Transparent so I can make thet main Panel have a round
                                              // border

        // -------------------------------------------------------------------------
        // Left Panel
        // -------------------------------------------------------------------------

        int baseInfoPanelWidth = 400;
        int infoPanelWidth = s(baseInfoPanelWidth);

        JLayeredPane infoPanel = new JLayeredPane() {
            @Override
            public void doLayout() {
                for (Component component : getComponents()) {
                    component.setBounds(0, 0, getWidth(), getHeight());
                }
            }
        };
        infoPanel.setPreferredSize(new Dimension(infoPanelWidth, panelHeight));
        infoPanel.setBackground(maroonDeep);
        infoPanel.setOpaque(false);

        // -------------------------------------------------------------------------
        // Left Window Panel
        // -------------------------------------------------------------------------

        JPanel leftWindowPanel = new JPanel();
        leftWindowPanel.setLayout(new BorderLayout());
        leftWindowPanel.setBackground(maroonDeep);
        leftWindowPanel.setOpaque(false);
        leftWindowPanel.setPreferredSize(new Dimension(infoPanelWidth, topBarHeight));

        TopPanel leftTopPanel = new TopPanel(this, scale, leftWindowPanel.getBackground());
        leftTopPanel.setOpaque(false);
        leftWindowPanel.add(leftTopPanel, BorderLayout.CENTER);

        LoginCarouselPanel carouselPanel = new LoginCarouselPanel();

        JPanel leftControlLayer = new JPanel(new BorderLayout());
        leftControlLayer.setOpaque(false);
        leftControlLayer.add(leftWindowPanel, BorderLayout.NORTH);

        // -------------------------------------------------------------------------
        // Left Panel Assembly
        // -------------------------------------------------------------------------

        infoPanel.add(carouselPanel, JLayeredPane.DEFAULT_LAYER);
        infoPanel.add(leftControlLayer, JLayeredPane.PALETTE_LAYER);

        // -------------------------------------------------------------------------
        // Login Panel
        // -------------------------------------------------------------------------

        int baseLoginPanelWidth = 600;
        int loginPanelWidth = s(baseLoginPanelWidth);

        JPanel loginPanel = new JPanel();
        loginPanel.setPreferredSize(new Dimension(loginPanelWidth, panelHeight));
        loginPanel.setBackground(darkGray);
        loginPanel.setLayout(new BorderLayout());

        // -------------------------------------------------------------------------
        // Right Top Bar
        // -------------------------------------------------------------------------
        int buttonFontSizes = s(16);

        JPanel rightWindowPanel = new JPanel();
        rightWindowPanel.setLayout(new BorderLayout());
        rightWindowPanel.setBackground(darkGray);
        rightWindowPanel.setPreferredSize(new Dimension(loginPanelWidth, topBarHeight));

        TopPanel rightTopPanel = new TopPanel(this, scale, lightGray, true, false, true,
                rightWindowPanel.getBackground(), buttonFontSizes, rightWindowPanel.getBackground(), true,
                new Color(0xFFFFFFFF), lightGray);
        rightWindowPanel.add(rightTopPanel, BorderLayout.CENTER);

        // -------------------------------------------------------------------------
        // Credentials Panel
        // -------------------------------------------------------------------------

        JPanel credentialsPanel = new JPanel();
        credentialsPanel.setBackground(darkGray);
        credentialsPanel.setLayout(new BoxLayout(credentialsPanel, BoxLayout.Y_AXIS));

        // -------------------------------------------------------------------------
        // Title Panel
        // -------------------------------------------------------------------------

        int titleFontSize = s(34);

        Dimension titlePanelSize = new Dimension(loginPanelWidth, s(125));

        // --- Logo ---
        Image scaled = new ImageIcon(getClass().getResource("/images/smallSeatendanceLogo.png")).getImage()
                .getScaledInstance(s(125), s(125), Image.SCALE_SMOOTH);

        JLabel logoLabel = new JLabel(new ImageIcon(scaled)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2d = (Graphics2D) g;
                g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
                super.paintComponent(g2d);
            }
        };

        // --- Title ---
        JLabel titleLabel = new JLabel("SEATENDANCE");
        titleLabel.setForeground(white);
        titleLabel.setFont(new Font("Impact", Font.PLAIN, titleFontSize));

        // --- Title Panel ---
        JPanel titlePanel = new JPanel();

        titlePanel.setLayout(new FlowLayout(FlowLayout.CENTER, s(10), 0));
        titlePanel.setOpaque(false);
        titlePanel.setAlignmentX(Component.CENTER_ALIGNMENT);
        titlePanel.setPreferredSize(titlePanelSize);
        titlePanel.setMinimumSize(titlePanelSize);
        titlePanel.setMaximumSize(titlePanelSize);

        titlePanel.add(logoLabel);
        titlePanel.add(titleLabel);

        // -------------------------------------------------------------------------
        // Fields Panel
        // -------------------------------------------------------------------------

        int usernamePasswordFontSize = s(20);
        int labelFontSize = s(16);
        int buttonFontSize = s(28);

        Dimension fieldSizes = new Dimension(s(484), s(47));
        Dimension buttonSizes = new Dimension(s(181), s(73));

        // --- Label ---
        JLabel label = new JLabel("Sign in with Email or RFID");
        label.setForeground(white);
        label.setAlignmentX(Component.CENTER_ALIGNMENT);
        label.setFont(new Font(null, Font.PLAIN, labelFontSize));

        // --- Email Field ---
        JTextField adminEmail = new JTextField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2D = (Graphics2D) g;

                RoundRectangle2D roundRectangle = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), s(47),
                        s(47));

                g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2D.setColor(getBackground());
                g2D.fill(roundRectangle);
                g2D.setClip(roundRectangle);
                super.paintComponent(g2D);
                g2D.setClip(null);
                g2D.setColor(lightGray);
                g2D.setStroke(new BasicStroke(s(2)));
                g2D.draw(roundRectangle);

                if (getText().length() == 0 && !isFocusOwner()) {
                    g2D.setColor(lightGray);
                    g2D.setFont(getFont());
                    g2D.drawString(" Email", s(12), getHeight() / 2 + g2D.getFontMetrics().getAscent() / 2 - 2);
                }

            }
        };
        adminEmail.setPreferredSize(fieldSizes);
        adminEmail.setMinimumSize(fieldSizes);
        adminEmail.setMaximumSize(fieldSizes);
        adminEmail.setAlignmentX(Component.CENTER_ALIGNMENT);
        adminEmail.setFont(new Font(null, Font.PLAIN, usernamePasswordFontSize));
        adminEmail.setBackground(darkGray2);
        adminEmail.setForeground(white);
        adminEmail.setCaretColor(white);
        adminEmail.setOpaque(false);
        adminEmail.setBorder(BorderFactory.createEmptyBorder(0, s(10), 0, s(10)));

        // --- Password Field ---
        JPasswordField adminPassword = new JPasswordField() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2D = (Graphics2D) g;

                RoundRectangle2D roundRectangle = new RoundRectangle2D.Float(0, 0, getWidth(), getHeight(), s(47),
                        s(47));

                g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2D.setColor(getBackground());
                g2D.fill(roundRectangle);
                g2D.setClip(roundRectangle);
                super.paintComponent(g2D);
                g2D.setClip(null);
                g2D.setColor(lightGray);
                g2D.setStroke(new BasicStroke(s(2)));
                g2D.draw(roundRectangle);

                if (getPassword().length == 0 && !isFocusOwner()) {
                    g2D.setColor(lightGray);
                    g2D.setFont(getFont());
                    g2D.drawString(" Password", s(12), getHeight() / 2 + g2D.getFontMetrics().getAscent() / 2 - 2);
                }
            }
        };
        adminPassword.setPreferredSize(fieldSizes);
        adminPassword.setMinimumSize(fieldSizes);
        adminPassword.setMaximumSize(fieldSizes);
        adminPassword.setAlignmentX(Component.CENTER_ALIGNMENT);
        adminPassword.setFont(new Font(null, Font.PLAIN, usernamePasswordFontSize));
        adminPassword.setBackground(darkGray2);
        adminPassword.setForeground(white);
        adminPassword.setCaretColor(white);
        adminPassword.setOpaque(false);
        adminPassword.setBorder(BorderFactory.createEmptyBorder(0, s(10), 0, s(10)));

        // --- FocusListeners ---

        adminEmail.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                adminPassword.repaint();
                adminEmail.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                adminPassword.repaint();
                adminEmail.repaint();
            }
        });

        adminPassword.addFocusListener(new FocusAdapter() {
            @Override
            public void focusGained(FocusEvent e) {
                adminEmail.repaint();
                adminPassword.repaint();
            }

            @Override
            public void focusLost(FocusEvent e) {
                adminEmail.repaint();
                adminPassword.repaint();
            }
        });

        // --- Login Button ---
        JButton loginButton = new JButton("Login") {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2D = (Graphics2D) g.create();

                g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

                // --- Buttons Shape
                RoundRectangle2D buttonShape = new RoundRectangle2D.Double(0, 0, getWidth(), getHeight(), 20, 20);

                g2D.setClip(buttonShape);
                g2D.setColor(getBackground());
                g2D.fill(buttonShape);

                super.paintComponent(g2D);

                g2D.setClip(null);
                g2D.setColor(lightGray);
                g2D.setStroke(new BasicStroke(s(2)));
                g2D.draw(buttonShape);

                g2D.dispose();

            }
        };
        loginButton.setBackground(darkGray2);
        loginButton.setForeground(white);
        loginButton.setPreferredSize(buttonSizes);
        loginButton.setMinimumSize(buttonSizes);
        loginButton.setMaximumSize(buttonSizes);
        loginButton.setFont(new Font(null, Font.PLAIN, buttonFontSize));
        loginButton.setFocusPainted(false);
        loginButton.setOpaque(false);
        loginButton.setContentAreaFilled(false);
        loginButton.setBorderPainted(false);
        loginButton.setAlignmentX(Component.CENTER_ALIGNMENT);

        // --- Error Label ---
        JLabel errorLabel = new JLabel(" ");
        errorLabel.setForeground(new Color(255, 100, 100)); // Red color
        errorLabel.setFont(new Font(null, Font.PLAIN, s(14)));
        errorLabel.setAlignmentX(Component.CENTER_ALIGNMENT);
        errorLabel.setVisible(false);

        JPanel errorPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        errorPanel.setOpaque(false);
        errorPanel.add(errorLabel);

        // --- Login Button Action ---
        ActionListener login = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String email = adminEmail.getText().trim();
                String password = new String(adminPassword.getPassword());

                String savedPassword = seatendanceApp.getDatabase().getUserPassword(email);

                if (savedPassword == null) {
                    errorLabel.setText("User not found");
                    errorLabel.setVisible(true);
                    System.out.println("User not found");
                } else if (BCrypt.checkpw(password, savedPassword)) {
                    RFIDManager rfidManager = seatendanceApp.getRFIDManager();
                    if (rfidManager != null) {
                        rfidManager.setScanListener(null);
                    }
                    dispose();
                    System.out.println("Login successful");
                    String role = seatendanceApp.getDatabase().getUserRole(email);
                    seatendanceApp.showSeatendance(email, role);
                } else {
                    errorLabel.setText("Invalid credentials");
                    errorLabel.setVisible(true);
                    System.out.println("Invalid credentials");
                }

            }
        };

        loginButton.addActionListener(login);
        // for the rfid scan - jibers
        RFIDManager rfidManager = seatendanceApp.getRFIDManager();
        if (rfidManager != null) {
            rfidManager.setScanListener(uid -> {
                DatabaseHandler.UserLoginInfo loginInfo = seatendanceApp.getDatabase().getUserLoginInfoByUID(uid);
                SwingUtilities.invokeLater(() -> {
                    if (loginInfo != null) {
                        rfidManager.setScanListener(null); // stop listening to avoid scanning other tags
                        dispose();
                        seatendanceApp.showSeatendance(loginInfo.email, loginInfo.role);
                    } else {
                        errorLabel.setText("Unregistered RFID tag");
                        errorLabel.setVisible(true);
                        System.out.println("User not found");
                    }
                });
            });
        }

        // --- Forgot Password? --- (Change this to a button)
        JLabel forgotPasswordLabel = new JLabel("<html><u>Forgot Password?</u></html>");
        forgotPasswordLabel.setFont(new Font(null, Font.PLAIN, labelFontSize));
        forgotPasswordLabel.setForeground(white);
        forgotPasswordLabel.setAlignmentX(Component.CENTER_ALIGNMENT);

        JPanel forgotPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 0, 0));
        forgotPanel.setOpaque(false);
        forgotPanel.add(forgotPasswordLabel);
        forgotPanel.setVisible(false);

        // --- Fields Panel ---
        JPanel fieldsPanel = new JPanel();
        fieldsPanel.setOpaque(false);
        fieldsPanel.setLayout(new BoxLayout(fieldsPanel, BoxLayout.Y_AXIS));
        fieldsPanel.setAlignmentX(Component.CENTER_ALIGNMENT);

        // -------------------------------------------------------------------------
        // Fields Panel Assembly
        // -------------------------------------------------------------------------

        fieldsPanel.add(label);
        fieldsPanel.add(Box.createVerticalStrut(s(10)));
        fieldsPanel.add(adminEmail);
        fieldsPanel.add(Box.createVerticalStrut(s(10)));
        fieldsPanel.add(adminPassword);
        fieldsPanel.add(Box.createVerticalStrut(s(10)));
        fieldsPanel.add(errorPanel);
        fieldsPanel.add(Box.createVerticalStrut(s(10)));
        fieldsPanel.add(loginButton);
        fieldsPanel.add(Box.createVerticalStrut(s(10)));
        fieldsPanel.add(forgotPanel);

        // -------------------------------------------------------------------------
        // Credentials Panel Assembly
        // -------------------------------------------------------------------------

        credentialsPanel.add(Box.createVerticalGlue());
        credentialsPanel.add(titlePanel);
        credentialsPanel.add(Box.createVerticalStrut(s(30)));
        credentialsPanel.add(fieldsPanel);
        credentialsPanel.add(Box.createVerticalGlue());

        // -------------------------------------------------------------------------
        // Credits Panel
        // -------------------------------------------------------------------------

        JPanel creditsPanel = new JPanel();
        creditsPanel.setBackground(darkGray);
        creditsPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));

        JLabel copyrightLabel = new JLabel("© Seatendance 2026");
        copyrightLabel.setForeground(white);

        creditsPanel.add(copyrightLabel);

        // -------------------------------------------------------------------------
        // Login Panel Assembly
        // -------------------------------------------------------------------------

        loginPanel.add(rightWindowPanel, BorderLayout.NORTH);
        loginPanel.add(credentialsPanel, BorderLayout.CENTER);
        loginPanel.add(creditsPanel, BorderLayout.SOUTH);

        // -------------------------------------------------------------------------
        // Main Panel
        // -------------------------------------------------------------------------

        JPanel mainPanel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2D = (Graphics2D) g;

                g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                super.paintComponent(g2D);
                g2D.setClip(frameShape);
                g2D.setColor(getBackground());
                g2D.fill(frameShape);
            }

            @Override
            protected void paintChildren(Graphics g) {
                Graphics2D g2D = (Graphics2D) g;

                g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2D.setClip(frameShape);
                super.paintChildren(g2D);

                // Draw border
                g2D.setClip(frameShape);
                g2D.setColor(lightGray);
                g2D.setStroke(new BasicStroke(s(4)));
                g2D.draw(frameShape);
            }
        };
        mainPanel.setPreferredSize(new Dimension(panelWidth, panelHeight));
        mainPanel.setLayout(new BorderLayout());
        mainPanel.setFocusable(true);
        mainPanel.setOpaque(false);

        // Need this so that the textfields lose focus when you press outside of it
        mainPanel.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                mainPanel.requestFocusInWindow();
            }
        });

        // -------------------------------------------------------------------------
        // Main Panel Assembly
        // -------------------------------------------------------------------------

        mainPanel.add(infoPanel, BorderLayout.WEST);
        mainPanel.add(loginPanel, BorderLayout.CENTER);

        // -------------------------------------------------------------------------
        // Frame Assembly
        // -------------------------------------------------------------------------

        setContentPane(mainPanel);
        pack();
        setLocationRelativeTo(null);
        setVisible(true);

    }

    private class LoginCarouselPanel extends JPanel {

        private static final int AUTO_SCROLL_DELAY = 4200;
        private static final int ANIMATION_DURATION = 720;

        private final Image[] slides;
        private final String[] captions = {
                "Connect with your class",
                "Track attendance efficiently",
                "Tap in with RFID technology"
        };
        private final Timer autoScrollTimer;
        private final Timer animationTimer;

        private int currentSlide = 0;
        private int nextSlide = 0;
        private long animationStartedAt;
        private double animationProgress = 0.0;
        private boolean animating = false;

        LoginCarouselPanel() {
            setOpaque(false);
            setBackground(maroonDeep);

            slides = loadSlides();

            autoScrollTimer = new Timer(AUTO_SCROLL_DELAY, e -> startNextSlide());
            autoScrollTimer.setInitialDelay(2600);

            animationTimer = new Timer(16, e -> updateAnimation());

            addHierarchyListener(e -> {
                if ((e.getChangeFlags() & HierarchyEvent.DISPLAYABILITY_CHANGED) != 0) {
                    if (isDisplayable()) {
                        autoScrollTimer.start();
                    } else {
                        autoScrollTimer.stop();
                        animationTimer.stop();
                    }
                }
            });
        }

        private Image[] loadSlides() {
            String[] paths = {
                    "/images/login-carousel/slide-3.png",
                    "/images/login-carousel/slide-2.png",
                    "/images/login-carousel/slide-1.png"
            };

            Image[] loadedSlides = new Image[paths.length];

            for (int i = 0; i < paths.length; i++) {
                URL slideResource = getClass().getResource(paths[i]);
                if (slideResource != null) {
                    loadedSlides[i] = new ImageIcon(slideResource).getImage();
                }
            }

            return loadedSlides;
        }

        private void startNextSlide() {
            if (slides.length <= 1 || animating) {
                return;
            }

            nextSlide = (currentSlide + 1) % slides.length;
            animationStartedAt = System.currentTimeMillis();
            animationProgress = 0.0;
            animating = true;
            animationTimer.start();
        }

        private void updateAnimation() {
            long elapsed = System.currentTimeMillis() - animationStartedAt;
            animationProgress = Math.min(1.0, elapsed / (double) ANIMATION_DURATION);

            if (animationProgress >= 1.0) {
                currentSlide = nextSlide;
                animationProgress = 0.0;
                animating = false;
                animationTimer.stop();
            }

            repaintLoginWindow();
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2D = (Graphics2D) g.create();
            g2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g2D.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g2D.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

            int width = getWidth();
            int height = getHeight();
            Shape oldClip = g2D.getClip();

            g2D.clip(createLeftRoundedClip(width, height));

            paintFallbackBackground(g2D, width, height);

            double easedProgress = easeInOut(animationProgress);
            int slideOffset = animating ? (int) Math.round(width * easedProgress) : 0;

            if (animating) {
                paintSlide(g2D, currentSlide, -slideOffset, width, height);
                paintSlide(g2D, nextSlide, width - slideOffset, width, height);
            } else {
                paintSlide(g2D, currentSlide, 0, width, height);
            }

            if (animating) {
                paintCaption(g2D, captions[currentSlide], -slideOffset, width, height, 1.0 - easedProgress);
                paintCaption(g2D, captions[nextSlide], width - slideOffset, width, height, easedProgress);
            } else {
                paintCaption(g2D, captions[currentSlide], 0, width, height, 1.0);
            }

            paintIndicators(g2D, width, height);

            g2D.setClip(oldClip);
            g2D.dispose();
        }

        private void repaintLoginWindow() {
            Window window = SwingUtilities.getWindowAncestor(this);
            if (window != null) {
                window.repaint();
            } else {
                repaint();
            }
        }

        private Shape createLeftRoundedClip(int width, int height) {
            return new RoundRectangle2D.Double(0, 0, width + 20, height, 20, 20);
        }

        private double easeInOut(double progress) {
            return progress < 0.5
                    ? 4.0 * progress * progress * progress
                    : 1.0 - Math.pow(-2.0 * progress + 2.0, 3.0) / 2.0;
        }

        private void paintFallbackBackground(Graphics2D g2D, int width, int height) {
            GradientPaint baseGradient = new GradientPaint(
                    0, 0, maroonDeep,
                    width, height, new Color(18, 18, 20));
            g2D.setPaint(baseGradient);
            g2D.fillRect(0, 0, width, height);
        }

        private void paintSlide(Graphics2D g2D, int slideIndex, int x, int width, int height) {
            if (slideIndex < 0 || slideIndex >= slides.length || slides[slideIndex] == null) {
                return;
            }

            Shape oldClip = g2D.getClip();
            g2D.clipRect(x, 0, width, height);

            Image slide = slides[slideIndex];
            int imageWidth = slide.getWidth(this);
            int imageHeight = slide.getHeight(this);

            if (imageWidth > 0 && imageHeight > 0) {
                double scaleFactor = Math.max(width / (double) imageWidth, height / (double) imageHeight);
                int drawWidth = (int) Math.ceil(imageWidth * scaleFactor);
                int drawHeight = (int) Math.ceil(imageHeight * scaleFactor);
                int drawX = x + (width - drawWidth) / 2;
                int drawY = (height - drawHeight) / 2;

                g2D.drawImage(slide, drawX, drawY, drawWidth, drawHeight, this);
            }

            g2D.setClip(oldClip);
        }

        private void paintCaption(Graphics2D g2D, String caption, int x, int width, int height, double alpha) {
            Composite oldComposite = g2D.getComposite();
            float captionAlpha = (float) Math.max(0.0, Math.min(1.0, alpha));
            g2D.setComposite(AlphaComposite.SrcOver.derive(captionAlpha));

            Font captionFont = new Font("SansSerif", Font.BOLD, s(22));
            g2D.setFont(captionFont);

            FontMetrics metrics = g2D.getFontMetrics();
            int captionWidth = metrics.stringWidth(caption);
            int captionX = x + (width - captionWidth) / 2;
            int captionY = height - s(82);

            g2D.setColor(new Color(0, 0, 0, 120));
            g2D.drawString(caption, captionX + s(2), captionY + s(2));

            g2D.setColor(new Color(255, 245, 245, 235));
            g2D.drawString(caption, captionX, captionY);

            g2D.setComposite(oldComposite);
        }

        private void paintIndicators(Graphics2D g2D, int width, int height) {
            int visibleSlide = animating && animationProgress > 0.45 ? nextSlide : currentSlide;
            int inactiveSize = s(8);
            int activeWidth = s(28);
            int dotHeight = s(8);
            int gap = s(9);
            int totalWidth = 0;

            for (int i = 0; i < slides.length; i++) {
                totalWidth += i == visibleSlide ? activeWidth : inactiveSize;
                if (i < slides.length - 1) {
                    totalWidth += gap;
                }
            }

            int x = (width - totalWidth) / 2;
            int y = height - s(42);

            for (int i = 0; i < slides.length; i++) {
                boolean active = i == visibleSlide;
                int dotWidth = active ? activeWidth : inactiveSize;
                Color dotColor = active
                        ? new Color(255, 245, 245, 235)
                        : new Color(255, 255, 255, 90);

                g2D.setColor(dotColor);
                g2D.fillRoundRect(x, y, dotWidth, dotHeight, dotHeight, dotHeight);
                x += dotWidth + gap;
            }
        }

    }

}
