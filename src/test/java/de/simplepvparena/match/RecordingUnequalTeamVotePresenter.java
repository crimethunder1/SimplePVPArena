package de.simplepvparena.match;

import java.util.ArrayList;
import java.util.List;

final class RecordingUnequalTeamVotePresenter implements UnequalTeamVotePresenter {

    private final List<PendingUnequalTeamVote> shownVotes = new ArrayList<>();
    private final List<PendingUnequalTeamVote> closedVotes = new ArrayList<>();

    private int refreshCount;

    @Override
    public void show(PendingUnequalTeamVote vote) {
        shownVotes.add(vote);
    }

    @Override
    public void refresh(PendingUnequalTeamVote vote) {
        refreshCount++;
    }

    @Override
    public void close(PendingUnequalTeamVote vote) {
        closedVotes.add(vote);
    }

    List<PendingUnequalTeamVote> shownVotes() {
        return shownVotes;
    }

    List<PendingUnequalTeamVote> closedVotes() {
        return closedVotes;
    }

    int refreshCount() {
        return refreshCount;
    }
}
