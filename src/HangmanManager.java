import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.TreeMap;
import java.util.ArrayList;

/**
 * Manages the details of EvilHangman. This class keeps
 * tracks of the possible words from a dictionary during
 * rounds of hangman, based on guesses so far.
 *
 */
public class HangmanManager {
    private final Set<String> dict;
    private boolean debugOn;
    private int wordLen;
    private int numGuesses;
    private HangmanDifficulty diff;
    private Set<String> eligibleWords;
    private Set<Character> guessesMade;
    private StringBuilder secretPattern;
    private int mediumCount;
    private int easyCount;

    private static final int EASY_CYCLE = 2;
    private static final int EASY_NTH_HARDEST = 1;
    private static final int MEDIUM_CYCLE = 4;
    private static final int MEDIUM_NTH_HARDEST = 1;

    /*
     * Create a new HangmanManager from the provided set of words and phrases.
     * pre: words != null, words.size() > 0
     * @param words A set with the words for this instance of Hangman.
     * @param debugOn true if we should print out debugging to System.out.
     */
    public HangmanManager(Set<String> words, boolean debugOn) {
        this.dict = new TreeSet<>();
        for (String w : words) {
            this.dict.add(w);
        }
        this.debugOn = debugOn;
        guessesMade = new TreeSet<>();
    }

    /**
     * Create a new HangmanManager from the provided set of words and phrases.
     * Debugging is off.
     * pre: words != null, words.size() > 0
     *
     * @param words A set with the words for this instance of Hangman.
     */
    public HangmanManager(Set<String> words) {
        this.dict = new TreeSet<>();
        for (String w : words) {
            this.dict.add(w);
        }
        this.debugOn = false;
        guessesMade = new TreeSet<>();
    }

    /**
     * Get the number of words in this HangmanManager of the given length.
     * pre: none
     *
     * @param length The given length to check.
     * @return the number of words in the original Dictionary
     * with the given length
     */
    public int numWords(int length) {
        int num = 0;
        Iterator<String> iter = this.dict.iterator();
        while (iter.hasNext()) {
            String check = iter.next();
            if (check.length() == length) {
                num++;
            }
        }
        return num;
    }

    /**
     * Get for a new round of Hangman. Think of a round as a
     * complete game of Hangman.
     *
     * @param wordLen    the length of the word to pick this time.
     *                   numWords(wordLen) > 0
     * @param numGuesses the number of wrong guesses before the
     *                   player loses the round. numGuesses >= 1
     * @param diff       The difficulty for this round.
     */
    public void prepForRound(int wordLen, int numGuesses, HangmanDifficulty diff) {
        eligibleWords = new TreeSet<>();
        this.wordLen = wordLen;
        this.numGuesses = numGuesses;
        this.diff = diff;
        mediumCount = 0;
        easyCount = 0;
        guessesMade.clear();
        secretPattern = new StringBuilder();
        for (int i = 0; i < wordLen; i++) {
            secretPattern.append("-");
        }
        Iterator<String> iter = this.dict.iterator();
        while (iter.hasNext()) {
            String check = iter.next();
            if (check.length() == wordLen) {
                eligibleWords.add(check);
            }
        }
    }

    /**
     * The number of words still possible (live) based on the guesses so far.
     * Guesses will eliminate possible words.
     *
     * @return the number of words that are still possibilities based on the
     * original dictionary and the guesses so far.
     */
    public int numWordsCurrent() {
        return eligibleWords.size();
    }

    /**
     * Get the number of wrong guesses the user has left in
     * this round (game) of Hangman.
     *
     * @return the number of wrong guesses the user has left
     * in this round (game) of Hangman.
     */
    public int getGuessesLeft() {
        return numGuesses;
    }

    /**
     * Return a String that contains the letters the user has guessed
     * so far during this round.
     * The characters in the String are in alphabetical order.
     * The String is in the form [let1, let2, let3, ... letN].
     * For example [a, c, e, s, t, z]
     *
     * @return a String that contains the letters the user
     * has guessed so far during this round.
     */
    public String getGuessesMade() {
        return guessesMade.toString();
    }

    /**
     * Check the status of a character.
     *
     * @param guess The characater to check.
     * @return true if guess has been used or guessed this round of Hangman,
     * false otherwise.
     */
    public boolean alreadyGuessed(char guess) {
        return guessesMade.contains(guess);
    }

    /**
     * Get the current pattern. The pattern contains '-''s for
     * unrevealed (or guessed) characters and the actual character
     * for "correctly guessed" characters.
     * Pre: secretPattern.length() > 0
     *
     * @return the current pattern.
     */
    public String getPattern() {
        if (secretPattern == null || secretPattern.length() == 0) {
            throw new IllegalArgumentException("Pattern has not been created yet.");
        }
        return secretPattern.toString();
    }

    /**
     * Update the game status (pattern, wrong guesses, word list),
     * based on the give guess.
     *
     * @param guess pre: !alreadyGuessed(ch), the current guessed character
     * @return return a tree map with the resulting patterns and the number of
     * words in each of the new patterns.
     * The return value is for testing and debugging purposes.
     */
    public TreeMap<String, Integer> makeGuess(char guess) {
        guess = Character.toLowerCase(guess);
        if (alreadyGuessed(guess)) {
            throw new IllegalStateException("This character has already been guessed.");
        }
        //add guess to set containing all guessed letters
        guessesMade.add(guess);
        //check dictionary letters, make patterns for guess based on eligible guesses
        // (pattern, all words matching) format
        Map<String, ArrayList<String>> patternFamilies = createPatterns(guess);
        //choose pattern based on difficulty
        String chosenPattern = choosePattern(patternFamilies);
        eligibleWords = new TreeSet<>(patternFamilies.get(chosenPattern));
        secretPattern = new StringBuilder(chosenPattern);
        //decrement guesses if guess was not in pattern
        if (!secretPattern.toString().contains(Character.toString(guess))) {
            numGuesses--;
        }
        TreeMap<String, Integer> resultingPatterns = new TreeMap<>();
        for (String s : patternFamilies.keySet()) {
            resultingPatterns.put(s, patternFamilies.get(s).size());
        }
        return resultingPatterns;
    }

    /**
     * Chooses a pattern from the given pattern families based on the current difficulty.
     * For EASY and MEDIUM difficulties, may pick the Nth hardest pattern depending on
     * the number of guesses so far and the configured cycle constants.
     * Pre: patternFamilies != null and !patternFamilies.isEmpty().
     *
     * @param patternFamilies a map of pattern strings to the list of matching words
     * @return the chosen pattern to use for updating the game
     */
    private String choosePattern(Map<String, ArrayList<String>> patternFamilies) {
        if (patternFamilies == null || patternFamilies.isEmpty()) {
            throw new IllegalArgumentException("List of families is empty.");
        }
        ArrayList<String> sortedPatterns = getPatternsSortedBySize(patternFamilies);
        int idx = 0;
        if (diff == HangmanDifficulty.MEDIUM) {
            mediumCount++;
            if (mediumCount % MEDIUM_CYCLE == 0 && sortedPatterns.size() > MEDIUM_NTH_HARDEST) {
                idx = MEDIUM_NTH_HARDEST;
            }
        } else if (diff == HangmanDifficulty.EASY) {
            easyCount++;
            if (easyCount % EASY_CYCLE == 0 && sortedPatterns.size() > EASY_NTH_HARDEST) {
                idx = EASY_NTH_HARDEST;
            }
        }
        idx = Math.min(idx, sortedPatterns.size() - 1);
        return sortedPatterns.get(idx);
    }

    /**
     * Gets a list of pattern strings sorted in descending order based on the
     * number of words in each pattern's family.
     * Pre: patternFamilies != null and !patternFamilies.isEmpty().
     *
     * @param patternFamilies a map of pattern strings to the list of matching words
     * @return pattern strings sorted from largest to smallest family size
     */
    private ArrayList<String> getPatternsSortedBySize(Map<String,
            ArrayList<String>> patternFamilies) {
        if (patternFamilies == null || patternFamilies.isEmpty()) {
            throw new IllegalArgumentException("List of families is empty.");
        }
        ArrayList<String> sortedFamilies = new ArrayList<>();
        for (String key : patternFamilies.keySet()) {
            sortedFamilies.add(key);
        }
        for (int i = 0; i < sortedFamilies.size() - 1; i++) {
            int maxIndex = i;
            for (int j = i + 1; j < sortedFamilies.size(); j++) {
                if (patternFamilies.get(sortedFamilies.get(j)).size() >
                        patternFamilies.get(sortedFamilies.get(maxIndex)).size()) {
                    maxIndex = j;
                }
            }
            if (maxIndex != i) {
                String temp = sortedFamilies.get(i);
                sortedFamilies.set(i, sortedFamilies.get(maxIndex));
                sortedFamilies.set(maxIndex, temp);
            }
        }
        return sortedFamilies;
    }

    /**
     * Creates pattern families based on the given guessed character.
     * Pre: eligibleWords != null and !eligibleWords.isEmpty().
     *
     * @param g the character that has been guessed
     * @return a map of pattern strings lists of words that match the corresponding pattern
     */
    private Map<String, ArrayList<String>> createPatterns(char g) {
        Iterator<String> iter = eligibleWords.iterator();
        Map<String, ArrayList<String>> wordFamilies = new TreeMap<>();
        while (iter.hasNext()) {
            String check = iter.next();
            StringBuilder pat = new StringBuilder();
            for (int i = 0; i < check.length(); i++) {
                if (check.charAt(i) == g) {
                    pat.append(g);
                } else {
                    pat.append(secretPattern.charAt(i));
                }
            }
            if (!(wordFamilies.containsKey(pat.toString()))) {
                ArrayList<String> family = new ArrayList<>();
                family.add(check);
                wordFamilies.put(pat.toString(), family);
            } else {
                wordFamilies.get(pat.toString()).add(check);
            }
        }
        return wordFamilies;
    }

    /**
     * Return the secret word this HangmanManager finally ended up
     * picking for this round.
     * If there are multiple possible words left one is selected at random.
     * <br> pre: numWordsCurrent() > 0
     *
     * @return return the secret word the manager picked.
     */
    //todo: needs to be truly random or?
    public String getSecretWord() {
        int index = (int) (Math.random() * eligibleWords.size());
        Iterator<String> iter = eligibleWords.iterator();
        for (int i = 0; i < index; i++) {
            iter.next();
        }
        return iter.next();
    }
}

