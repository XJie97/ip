package tkit;

/** Display status of a task with a single-character icon. */
enum Status {
    DONE("X"),
    NOT_DONE(" ");

    private final String stateIcon;

    Status(String stateIcon) {
        assert stateIcon != null && stateIcon.length() == 1 : "State icon must be exactly one char";
        this.stateIcon = stateIcon;
    }

    /** Returns the one-character state icon used in list rendering. */
    public String stateIcon() {

        return stateIcon;
    }
}
