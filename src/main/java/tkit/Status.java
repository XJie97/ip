package tkit;

/** Display status of a task with a single-character icon. */
enum Status {
    DONE("X"),
    NOT_DONE(" ");

    private final String stateIcon;

    Status(String stateIcon) {
        this.stateIcon = stateIcon;
    }

    /** Returns the one-character state icon used in list rendering. */
    public String stateIcon() {
        return stateIcon;
    }
}
