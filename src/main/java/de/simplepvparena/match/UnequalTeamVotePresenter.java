package de.simplepvparena.match;

public interface UnequalTeamVotePresenter {

    UnequalTeamVotePresenter NONE = new UnequalTeamVotePresenter() {

        @Override
        public void show(PendingUnequalTeamVote vote) {
        }

        @Override
        public void refresh(PendingUnequalTeamVote vote) {
        }

        @Override
        public void close(PendingUnequalTeamVote vote) {
        }
    };

    void show(PendingUnequalTeamVote vote);

    void refresh(PendingUnequalTeamVote vote);

    void close(PendingUnequalTeamVote vote);
}
