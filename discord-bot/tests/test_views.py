from bot.storage import Event, Signup
from bot.views import (
    JOIN_BUTTON_ID,
    LEAVE_BUTTON_ID,
    EventMessageView,
    SignupDispatchView,
    format_signup_list,
)


class NoActions:
    async def open_signup_form(self, interaction):
        pass

    async def submit_signup(self, interaction, event_id, name, partner):
        pass

    async def withdraw_signup(self, interaction):
        pass


def make_event(is_open=True, is_active=True):
    return Event(
        id=1,
        guild_id=10,
        channel_id=20,
        message_id=30,
        title="Sommer Cup",
        description="x" * 1500,
        event_date="Samstag 20:00",
        is_open=is_open,
        is_active=is_active,
    )


def make_signups(count):
    return [Signup(i, 1, i, f"Player_{i:09d}", f"Mate_{i:011d}") for i in range(count)]


def buttons(view):
    return {item.custom_id: item for item in view.walk_children() if getattr(item, "custom_id", None)}


def test_open_event_has_enabled_buttons_and_is_persistent():
    view = EventMessageView(NoActions(), make_event(), make_signups(3), 2)
    found = buttons(view)
    assert set(found) == {JOIN_BUTTON_ID, LEAVE_BUTTON_ID}
    assert not any(button.disabled for button in found.values())
    assert view.is_persistent()


def test_closed_event_disables_buttons():
    view = EventMessageView(NoActions(), make_event(is_open=False), [], 2)
    assert all(button.disabled for button in buttons(view).values())


def test_archived_event_has_no_buttons():
    view = EventMessageView(NoActions(), make_event(is_active=False), [], 2)
    assert buttons(view) == {}


def test_long_list_is_truncated_within_discord_limits():
    view = EventMessageView(NoActions(), make_event(), make_signups(500), 2)
    assert view.content_length() <= 4000
    assert "weitere" in format_signup_list(make_signups(500))


def test_names_are_markdown_escaped():
    text = format_signup_list([Signup(1, 1, 1, "a_b_c", None)])
    assert "a\\_b\\_c" in text


def test_dispatch_view_is_persistent():
    assert SignupDispatchView(NoActions()).is_persistent()
