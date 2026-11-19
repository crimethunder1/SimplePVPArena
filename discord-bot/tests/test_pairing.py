import pytest

from bot.export import export_filename, make_slug, render_export, summarize
from bot.names import InvalidSignup, clean_signup_input, is_valid_minecraft_name
from bot.pairing import Registration, build_team_lines


def lines_for(*registrations: tuple[str, str | None]) -> list[tuple[str, ...]]:
    return build_team_lines(Registration(name, partner) for name, partner in registrations)


def test_mutual_partners_become_one_line():
    assert lines_for(("Steve", "Alex"), ("Alex", "Steve")) == [("Steve", "Alex")]


def test_mutual_partners_compare_case_insensitively_and_keep_own_spelling():
    assert lines_for(("Steve", "alex"), ("Alex", "STEVE")) == [("Steve", "Alex")]


def test_unregistered_partner_is_brought_along():
    assert lines_for(("Steve", "Alex")) == [("Steve", "Alex")]


def test_unregistered_partner_keeps_spelling_from_registrant():
    assert lines_for(("Steve", "aLeX")) == [("Steve", "aLeX")]


def test_only_earliest_registration_gets_unregistered_partner():
    assert lines_for(("Steve", "Alex"), ("Notch", "alex"), ("Jeb_", "Alex")) == [
        ("Steve", "Alex"),
        ("Notch",),
        ("Jeb_",),
    ]


def test_partner_who_named_someone_else_leaves_registrant_solo():
    assert lines_for(("Steve", "Alex"), ("Alex", "Notch"), ("Notch", "Alex")) == [
        ("Steve",),
        ("Alex", "Notch"),
    ]


def test_partner_who_registered_solo_leaves_registrant_solo():
    assert lines_for(("Steve", "Alex"), ("Alex", None)) == [("Steve",), ("Alex",)]


def test_partner_who_named_an_unregistered_player_leaves_registrant_solo():
    assert lines_for(("Steve", "Alex"), ("Alex", "Herobrine")) == [("Steve",), ("Alex", "Herobrine")]


def test_solo_registration_stays_solo():
    assert lines_for(("Notch", None), ("Dinnerbone", "")) == [("Notch",), ("Dinnerbone",)]


def test_self_partner_is_treated_as_solo():
    assert lines_for(("Steve", "steve")) == [("Steve",)]


def test_registered_player_named_later_by_someone_else_is_not_duplicated():
    assert lines_for(("Alex", None), ("Steve", "Alex")) == [("Alex",), ("Steve",)]


def test_order_follows_registration_time():
    result = lines_for(
        ("Notch", None),
        ("Alex", "Steve"),
        ("Herobrine", "Jeb_"),
        ("Steve", "Alex"),
        ("Dinnerbone", None),
    )
    assert result == [("Notch",), ("Alex", "Steve"), ("Herobrine", "Jeb_"), ("Dinnerbone",)]


def test_duplicate_registered_names_keep_only_the_first():
    assert lines_for(("Steve", None), ("steve", "Alex")) == [("Steve",)]


def test_no_name_appears_twice_in_mixed_scenario():
    result = lines_for(
        ("A_one", "B_two"),
        ("C_three", "B_two"),
        ("B_two", "C_three"),
        ("D_four", "E_five"),
        ("E_five", "D_four"),
        ("F_six", "a_one"),
        ("G_seven", "Unknown"),
        ("H_eight", "unknown"),
    )
    names = [name.lower() for line in result for name in line]
    assert len(names) == len(set(names))
    assert result == [
        ("A_one",),
        ("C_three", "B_two"),
        ("D_four", "E_five"),
        ("F_six",),
        ("G_seven", "Unknown"),
        ("H_eight",),
    ]


def test_example_from_contract():
    result = lines_for(
        ("Steve", "Alex"),
        ("Notch", None),
        ("Herobrine", "Jeb_"),
        ("Alex", "Steve"),
        ("Dinnerbone", None),
    )
    assert render_export(result) == "Steve Alex\nNotch\nHerobrine Jeb_\nDinnerbone\n"


def test_empty_export():
    assert render_export([]) == ""


def test_summary_counts_duos_solos_and_players():
    summary = summarize([("Steve", "Alex"), ("Notch",), ("Herobrine", "Jeb_"), ("Dinnerbone",)])
    assert (summary.duos, summary.solos, summary.players) == (2, 2, 6)


@pytest.mark.parametrize(
    ("title", "expected"),
    [
        ("Großes PvP Turnier #3", "grosses-pvp-turnier-3"),
        ("Über-Cup", "ueber-cup"),
        ("!!!", "event-7"),
    ],
)
def test_slug(title, expected):
    assert make_slug(title, "event-7") == expected


def test_export_filename():
    assert export_filename("Sommer Cup 2v2", 1) == "teams-sommer-cup-2v2.txt"


@pytest.mark.parametrize("name", ["Steve", "abc", "Jeb_", "A1234567890123_5"])
def test_valid_minecraft_names(name):
    assert is_valid_minecraft_name(name)


@pytest.mark.parametrize("name", ["ab", "A12345678901234567", "Steve Alex", "Stéve", "na-me", ""])
def test_invalid_minecraft_names(name):
    assert not is_valid_minecraft_name(name)


def test_clean_signup_input_strips_and_drops_empty_partner():
    assert clean_signup_input("  Steve ", "   ") == ("Steve", None)
    assert clean_signup_input("Steve", " Alex ") == ("Steve", "Alex")


def test_clean_signup_input_rejects_self_partner():
    with pytest.raises(InvalidSignup):
        clean_signup_input("Steve", "STEVE")


@pytest.mark.parametrize(("name", "partner"), [("x", None), ("Steve", "a b"), ("Steve", "ab")])
def test_clean_signup_input_rejects_invalid_names(name, partner):
    with pytest.raises(InvalidSignup):
        clean_signup_input(name, partner)
