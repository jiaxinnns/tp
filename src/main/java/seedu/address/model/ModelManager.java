package seedu.address.model;

import static java.util.Objects.requireNonNull;
import static seedu.address.commons.util.CollectionUtil.requireAllNonNull;

import java.nio.file.Path;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Predicate;
import java.util.logging.Logger;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import seedu.address.commons.core.GuiSettings;
import seedu.address.commons.core.LogsCenter;
import seedu.address.logic.commands.Command;
import seedu.address.logic.commands.UndoCommand;
import seedu.address.logic.commands.UndoableCommand;
import seedu.address.model.person.Person;
import seedu.address.model.tag.Tag;

/**
 * Represents the in-memory model of the address book data.
 */
public class ModelManager implements Model {
    private static final Logger logger = LogsCenter.getLogger(ModelManager.class);
    private final AddressBook addressBook;
    private final UserPrefs userPrefs;
    private FilteredList<Person> filteredPersons;
    private ObservableList<Tag> tagList;
    private Command previousCommand;

    /**
     * Initializes a ModelManager with the given addressBook and userPrefs.
     */
    public ModelManager(ReadOnlyAddressBook addressBook, ReadOnlyUserPrefs userPrefs) {
        requireAllNonNull(addressBook, userPrefs);

        logger.fine("Initializing with address book: " + addressBook + " and user prefs " + userPrefs);

        this.addressBook = new AddressBook(addressBook);
        this.userPrefs = new UserPrefs(userPrefs);
        filteredPersons = new FilteredList<>(this.addressBook.getPersonList());
        tagList = this.addressBook.getTagList();
    }

    public ModelManager() {
        this(new AddressBook(), new UserPrefs());
    }

    //=========== UserPrefs ==================================================================================

    @Override
    public void setUserPrefs(ReadOnlyUserPrefs userPrefs) {
        requireNonNull(userPrefs);
        this.userPrefs.resetData(userPrefs);
    }

    @Override
    public ReadOnlyUserPrefs getUserPrefs() {
        return userPrefs;
    }

    @Override
    public GuiSettings getGuiSettings() {
        return userPrefs.getGuiSettings();
    }

    @Override
    public void setGuiSettings(GuiSettings guiSettings) {
        requireNonNull(guiSettings);
        userPrefs.setGuiSettings(guiSettings);
    }

    @Override
    public Path getAddressBookFilePath() {
        return userPrefs.getAddressBookFilePath();
    }

    @Override
    public void setAddressBookFilePath(Path addressBookFilePath) {
        requireNonNull(addressBookFilePath);
        userPrefs.setAddressBookFilePath(addressBookFilePath);
    }

    //=========== AddressBook ================================================================================

    @Override
    public void setAddressBook(ReadOnlyAddressBook addressBook) {
        this.addressBook.resetData(addressBook);
    }

    @Override
    public ReadOnlyAddressBook getAddressBook() {
        return addressBook;
    }

    @Override
    public boolean hasPerson(Person person) {
        requireNonNull(person);
        return addressBook.hasPerson(person);
    }

    @Override
    public void deletePerson(Person target) {
        addressBook.removePerson(target);
    }

    @Override
    public void addPerson(Person person) {
        addressBook.addPerson(person);
        updateFilteredPersonList(PREDICATE_SHOW_ALL_PERSONS);
    }

    @Override
    public void setPerson(Person target, Person editedPerson) {
        requireAllNonNull(target, editedPerson);

        addressBook.setPerson(target, editedPerson);
    }

    //=========== Filtered Person List Accessors =============================================================

    /**
     * Returns an unmodifiable view of the list of {@code Person} backed by the internal list of
     * {@code versionedAddressBook}
     */
    @Override
    public ObservableList<Person> getFilteredPersonList() {
        return filteredPersons;
    }

    /**
     * Returns an unmodifiable view of the full list of {@code Person} backed by the internal list of
     * {@code versionedAddressBook}
     */
    public ObservableList<Person> getFullPersonList() {
        return addressBook.getPersonList();
    }

    @Override
    public void updateFilteredPersonList(Predicate<Person> predicate) {
        requireNonNull(predicate);

        @SuppressWarnings("unchecked")
        Predicate<Person> currentPredicate = (Predicate<Person>) filteredPersons.getPredicate();

        if (currentPredicate == null || predicate.equals(PREDICATE_SHOW_ALL_PERSONS)) {
            filteredPersons.setPredicate(predicate);
        } else {
            filteredPersons.setPredicate(currentPredicate.and(predicate));
        }
    }

    //=========== Tags ================================================================================

    @Override
    public boolean addTag(Tag tag) {
        return addressBook.addTag(tag);
    }

    @Override
    public boolean addTags(List<Tag> tags) {
        boolean isSuccessful = true;
        for (Tag tag : tags) {
            isSuccessful &= addressBook.addTag(tag);
        }
        return isSuccessful;
    }

    @Override public boolean deleteTag(Tag tag) {
        return addressBook.deleteTag(tag);
    }

    @Override
    public boolean deleteTags(List<Tag> tags) {
        boolean isSuccessful = true;
        for (Tag tag : tags) {
            isSuccessful &= addressBook.deleteTag(tag);
        }
        return isSuccessful;
    }

    @Override
    public boolean renameTag(Tag existingTag, String newTagName) {
        boolean isSuccessful = addressBook.renameTag(existingTag, newTagName);
        return isSuccessful;
    }

    @Override
    public boolean hasTag(Tag tag) {
        return addressBook.hasTag(tag);
    }

    @Override
    public String getTagList() {
        return addressBook.tagsToString();
    }

    @Override
    public Set<Tag> getTagsInUse() {
        Set<Tag> tagsInUse = new HashSet<>();
        List<Person> persons = getFullPersonList();
        for (Person person : persons) {
            Set<Tag> personTags = person.getTags();
            tagsInUse.addAll(personTags);
        }
        return tagsInUse;
    }

    @Override
    public void removeTagFromPersons(Tag tag) {
        List<Person> persons = getFullPersonList();
        for (Person person : persons) {
            Set<Tag> newTags = new HashSet<>(person.getTags());
            newTags.remove(tag);

            Person updatedPerson = new Person(person.getName(), person.getPhone(),
                    person.getEmail(), person.getRsvpStatus(), newTags);
            setPerson(person, updatedPerson);
        }
    }

    @Override
    public void editTagInPersons(Tag existingTag, String newTagName) {
        List<Person> persons = getFullPersonList();
        for (Person person : persons) {
            Set<Tag> tags = new HashSet<>(person.getTags());
            for (Tag tag : tags) {
                if (tag.equals(existingTag)) {
                    tag.setTagName(newTagName);
                }
            }
        }
    }

    @Override
    public boolean checkAcceptableTagListSize(int additionalTags) {
        return addressBook.checkAcceptableTagListSize(additionalTags);
    }

    @Override
    public ObservableList<Tag> getTagListAsObservableList() {
        return addressBook.getTagList();
    }

    @Override
    public void updateTagList() {
        ObservableList<Tag> tl = this.addressBook.getTagList();
        tagList.setAll(FXCollections.observableArrayList(tl));
    }

    @Override
    public void updatePreviousCommand(Command nextCommand) {
        this.previousCommand = nextCommand;
    }

    @Override
    public Command getPreviousCommand() {
        return this.previousCommand;
    }

    @Override
    public Predicate<Person> getCurrentPredicate() {
        @SuppressWarnings("unchecked")
        Predicate<Person> result = (Predicate<Person>) filteredPersons.getPredicate();
        return result;
    }

    @Override
    public boolean equals(Object other) {
        if (other == this) {
            return true;
        }

        // instanceof handles nulls
        if (!(other instanceof ModelManager)) {
            return false;
        }

        ModelManager otherModelManager = (ModelManager) other;
        return addressBook.equals(otherModelManager.addressBook)
                && userPrefs.equals(otherModelManager.userPrefs)
                && filteredPersons.equals(otherModelManager.filteredPersons);
    }
}
