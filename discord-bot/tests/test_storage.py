import pytest

from bot.storage import SignupOutcome, Storage

GUILD = 1000
CHANNEL = 2000


@pytest.fixture
def storage(tmp_path):
    store = Storage(tmp_path / "nested" / "signups.db")
    yield store
    store.close()


def active_event(storage, title="Cup", message_id=1):
    event = storage.create_event(GUILD, CHANNEL, title, "Beschreibung", "Samstag")
    return storage.activate_event(event.id, message_id)


def test_new_event_is_inactive_until_activated(storage):
    event = storage.create_event(GUILD, CHANNEL, "Cup")
    assert not event.is_active
    assert event.is_open
    assert storage.active_event(GUILD) is None
    activated = storage.activate_event(event.id, 55)
    assert activated.is_active
    assert activated.message_id == 55
    assert storage.active_event(GUILD) == activated


def test_activating_new_event_archives_old_one(storage):
    old = active_event(storage, "Alt", 1)
    new = active_event(storage, "Neu", 2)
    assert storage.active_event(GUILD).id == new.id
    assert not storage.get_event(old.id).is_active
    assert [event.id for event in storage.active_events()] == [new.id]


def test_events_of_other_guilds_stay_active(storage):
    other = storage.activate_event(storage.create_event(GUILD + 1, CHANNEL, "Andere").id, 9)
    active_event(storage)
    assert storage.get_event(other.id).is_active


def test_delete_event_removes_signups(storage):
    event = active_event(storage)
    storage.save_signup(event.id, 1, "Steve", None)
    storage.delete_event(event.id)
    assert storage.get_event(event.id) is None
    assert storage.signups(event.id) == []


def test_open_and_close(storage):
    event = active_event(storage)
    assert not storage.set_open(event.id, False).is_open
    assert storage.set_open(event.id, True).is_open


def test_signup_created_and_listed_in_order(storage):
    event = active_event(storage)
    assert storage.save_signup(event.id, 1, "Steve", "Alex") is SignupOutcome.CREATED
    assert storage.save_signup(event.id, 2, "Notch", None) is SignupOutcome.CREATED
    signups = storage.signups(event.id)
    assert [(s.minecraft_name, s.partner_name, s.discord_user_id) for s in signups] == [
        ("Steve", "Alex", 1),
        ("Notch", None, 2),
    ]


def test_same_user_updates_entry_and_keeps_position(storage):
    event = active_event(storage)
    storage.save_signup(event.id, 1, "Steve", None)
    storage.save_signup(event.id, 2, "Notch", None)
    assert storage.save_signup(event.id, 1, "Steve2", "Alex") is SignupOutcome.UPDATED
    signups = storage.signups(event.id)
    assert [(s.minecraft_name, s.partner_name) for s in signups] == [("Steve2", "Alex"), ("Notch", None)]


def test_same_user_can_change_spelling_of_own_name(storage):
    event = active_event(storage)
    storage.save_signup(event.id, 1, "steve", None)
    assert storage.save_signup(event.id, 1, "Steve", None) is SignupOutcome.UPDATED
    assert storage.signups(event.id)[0].minecraft_name == "Steve"


def test_other_user_cannot_take_registered_name(storage):
    event = active_event(storage)
    storage.save_signup(event.id, 1, "Steve", None)
    assert storage.save_signup(event.id, 2, "STEVE", None) is SignupOutcome.NAME_TAKEN
    assert len(storage.signups(event.id)) == 1


def test_user_cannot_rename_to_name_of_someone_else(storage):
    event = active_event(storage)
    storage.save_signup(event.id, 1, "Steve", None)
    storage.save_signup(event.id, 2, "Alex", None)
    assert storage.save_signup(event.id, 2, "steve", None) is SignupOutcome.NAME_TAKEN
    assert storage.signup_for_user(event.id, 2).minecraft_name == "Alex"


def test_same_name_allowed_in_different_events(storage):
    first = active_event(storage, "Eins", 1)
    storage.save_signup(first.id, 1, "Steve", None)
    second = active_event(storage, "Zwei", 2)
    assert storage.save_signup(second.id, 2, "Steve", None) is SignupOutcome.CREATED


def test_remove_signup_for_user(storage):
    event = active_event(storage)
    storage.save_signup(event.id, 1, "Steve", None)
    removed = storage.remove_signup_for_user(event.id, 1)
    assert removed.minecraft_name == "Steve"
    assert storage.signups(event.id) == []
    assert storage.remove_signup_for_user(event.id, 1) is None


def test_remove_signup_by_name_is_case_insensitive(storage):
    event = active_event(storage)
    storage.save_signup(event.id, 1, "Steve", None)
    assert storage.remove_signup_by_name(event.id, " steve ").minecraft_name == "Steve"
    assert storage.remove_signup_by_name(event.id, "Steve") is None


def test_reregistering_after_removal_moves_to_the_end(storage):
    event = active_event(storage)
    storage.save_signup(event.id, 1, "Steve", None)
    storage.save_signup(event.id, 2, "Alex", None)
    storage.remove_signup_for_user(event.id, 1)
    storage.save_signup(event.id, 1, "Steve", None)
    assert [s.minecraft_name for s in storage.signups(event.id)] == ["Alex", "Steve"]


def test_data_survives_reopening_the_database(tmp_path):
    path = tmp_path / "signups.db"
    first = Storage(path)
    event = first.activate_event(first.create_event(GUILD, CHANNEL, "Cup").id, 77)
    first.save_signup(event.id, 1, "Steve", "Alex")
    first.close()
    second = Storage(path)
    try:
        assert second.active_event(GUILD).message_id == 77
        assert second.signups(event.id)[0].partner_name == "Alex"
    finally:
        second.close()
