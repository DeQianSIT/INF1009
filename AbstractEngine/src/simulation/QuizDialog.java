package simulation;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Modal quiz dialog with 30-second countdown.
 * Returns true if the user answered correctly, false otherwise (wrong answer or timeout).
 */
public class QuizDialog extends JDialog {
    private static final int COUNTDOWN_SECONDS = 30;

    private int selectedAnswer = -1;
    private int correctIndex;
    private int countdown = COUNTDOWN_SECONDS;
    private Timer countdownTimer;
    private JLabel countdownLabel;
    private JButton[] answerButtons;
    private volatile boolean answered = false;

    public QuizDialog(Frame parent, String question, String[] options, int correctIndex) {
        super(parent, "Quick Quiz!", true);
        this.correctIndex = correctIndex;

        setLayout(new BorderLayout(15, 15));
        setSize(500, 320);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);

        JPanel top = new JPanel();
        top.setBackground(new Color(30, 30, 50));
        JLabel qLabel = new JLabel("<html><center>" + question + "</center></html>");
        qLabel.setFont(new Font("Arial", Font.BOLD, 16));
        qLabel.setForeground(Color.WHITE);
        qLabel.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));
        top.add(qLabel);
        add(top, BorderLayout.NORTH);

        countdownLabel = new JLabel(COUNTDOWN_SECONDS + " seconds");
        countdownLabel.setFont(new Font("Arial", Font.BOLD, 14));
        countdownLabel.setForeground(Color.YELLOW);
        countdownLabel.setHorizontalAlignment(SwingConstants.CENTER);
        countdownLabel.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
        add(countdownLabel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new GridLayout(2, 2, 10, 10));
        buttonPanel.setBackground(new Color(40, 40, 60));
        buttonPanel.setBorder(BorderFactory.createEmptyBorder(10, 20, 20, 20));

        answerButtons = new JButton[options.length];
        for (int i = 0; i < options.length; i++) {
            final int idx = i;
            JButton btn = new JButton(options[i]);
            btn.setFont(new Font("Arial", Font.PLAIN, 14));
            btn.setFocusPainted(false);
            btn.addActionListener(e -> selectAnswer(idx));
            answerButtons[i] = btn;
            buttonPanel.add(btn);
        }
        add(buttonPanel, BorderLayout.SOUTH);

        countdownTimer = new Timer(true);
        countdownTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(() -> {
                    if (answered) {
                        countdownTimer.cancel();
                        return;
                    }
                    countdown--;
                    countdownLabel.setText(countdown + " seconds");
                    if (countdown <= 10) {
                        countdownLabel.setForeground(Color.RED);
                    }
                    if (countdown <= 0) {
                        answered = true;
                        countdownTimer.cancel();
                        SwingUtilities.invokeLater(() -> dispose());
                    }
                });
            }
        }, 1000, 1000);
    }

    private void selectAnswer(int index) {
        if (answered) return;
        answered = true;
        selectedAnswer = index;
        countdownTimer.cancel();
        dispose();
    }

    /**
     * Show the quiz and return true if answered correctly, false otherwise.
     */
    public static boolean showQuiz(Frame parent, String question, String[] options, int correctIndex) {
        QuizDialog dialog = new QuizDialog(parent, question, options, correctIndex);
        dialog.setVisible(true);
        return dialog.selectedAnswer == dialog.correctIndex;
    }
}
