package br.com.caelum.brutal.model;

import static br.com.caelum.brutal.sanitizer.QuotesSanitizer.sanitize;
import static javax.persistence.FetchType.EAGER;
import static org.hibernate.annotations.CascadeType.SAVE_UPDATE;

import java.util.ArrayList;
import java.util.List;

import javax.persistence.Cacheable;
import javax.persistence.Embedded;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinTable;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.validation.constraints.NotNull;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Cascade;
import org.hibernate.annotations.Type;
import org.joda.time.DateTime;

import br.com.caelum.brutal.model.interfaces.Moderatable;
import br.com.caelum.brutal.model.interfaces.RssContent;
import br.com.caelum.brutal.model.interfaces.Taggable;
import br.com.caelum.brutal.model.interfaces.ViewCountable;
import br.com.caelum.brutal.model.interfaces.Votable;
import br.com.caelum.brutal.model.interfaces.Watchable;
import br.com.caelum.brutal.model.watch.Watcher;

@Cacheable
@Cache(usage=CacheConcurrencyStrategy.READ_WRITE, region="cache")
@Entity
public class Question extends Moderatable implements Post, Taggable, ViewCountable, Watchable, RssContent {
	@Id
	@GeneratedValue
	private Long id;

	@ManyToOne(optional = false, fetch = EAGER)
	@Cascade(SAVE_UPDATE)
	@NotNull
	private QuestionInformation information = null;
	
	@OneToMany(mappedBy="question")
	@Cascade(SAVE_UPDATE)
	private List<QuestionInformation> history = new ArrayList<>();
	
	@Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
	private final DateTime createdAt = new DateTime();

	@Type(type = "org.joda.time.contrib.hibernate.PersistentDateTime")
	private DateTime lastUpdatedAt = new DateTime();

	@ManyToOne
	private User lastTouchedBy = null;

	@ManyToOne(optional = true, fetch = EAGER)
	private Answer solution;

	@ManyToOne(fetch = EAGER)
	private User author;

	@BatchSize(size = 10)
	@OneToMany(mappedBy = "question")
	private final List<Answer> answers = new ArrayList<>();

	private long answerCount = 0l;

	private long views = 0l;
	
	@JoinTable(name = "Question_Votes")
	@OneToMany
	private final List<Vote> votes = new ArrayList<>();

	private long voteCount = 0l;

	@Embedded
	private final QuestionCommentList comments = new QuestionCommentList();
	
	@JoinTable(name = "Question_Flags")
	@OneToMany
	private final List<Flag> flags = new ArrayList<>();
	
	@Embedded
	private final ModerationOptions moderationOptions = new ModerationOptions();

	@JoinTable(name = "Question_Watchers")
	@OneToMany
	private final List<Watcher> watchers = new ArrayList<>();
	
    public static final long SPAM_BOUNDARY = -5;
    
	
	/**
	 * @deprecated hibernate eyes only
	 */
	public Question() {
		this.information = null;
	}

	public Question(QuestionInformation questionInformation, User author) {
		this.author = author;
		enqueueChange(questionInformation, UpdateStatus.NO_NEED_TO_APPROVE);
	}

	@Override
	public String toString() {
		return "Question [title=" + information.getTitle() + ", createdAt=" + createdAt + "]";
	}

	public void setAuthor(User author) {
		if (this.author != null)
			return;
		this.author = author;
		touchedBy(author);
	}

	public void touchedBy(User author) {
		this.lastTouchedBy = author;
		this.lastUpdatedAt = new DateTime();
	}

	void setId(Long id) {
		this.id = id;
	}

	public Long getId() {
		return id;
	}
	
	public void add(Answer a) {
		if(a.getId() != null && answers.contains(a)){
			throw new IllegalStateException("This question already have the answer with id: "+ a.getId());
		}
	    answers.add(a);
	    answerCount++;
	}

	public long getViews() {
		return views;
	}

	public void setViews(long views) {
		this.views = views;
	}

	public DateTime getLastUpdatedAt() {
		return lastUpdatedAt;
	}

	public User getLastTouchedBy() {
		return lastTouchedBy;
	}

	public List<Answer> getAnswers() {
		return answers;
	}

	@Override
	public Question getMainThread() {
		return this; //sorry, I need to get a Question from a Comment.
	}
	
	public void ping() {
		this.views++;
	}

	protected void markAsSolvedBy(Answer answer) {
		if (!answer.getMainThread().equals(this))
			throw new RuntimeException("Can not be solved by this answer");
		this.solution = answer;
	}

	protected void removeSolution() {
		this.solution = null;
	}
	
	public Answer getSolution() {
		return solution;
	}

	public long getAnswersCount() {
		return answerCount;
	}

	public boolean isSolved() {
		return solution != null;
	}

	@Override
	public void substitute(Vote previous, Vote vote) {
		this.voteCount += vote.substitute(previous, votes);
	}

	@Override
	public long getVoteCount() {
		return voteCount;
	}

	public User getAuthor() {
		return author;
	}

	@Override
	public Comment add(Comment comment) {
		this.comments.add(comment);
		return comment;
	}
	
	@Override
	public List<Comment> getVisibleCommentsFor(User user) {
		return comments.getVisibleCommentsFor(user);
	}

	public String getTitle() {
		return information.getTitle();
	}

	public String getDescription() {
		return information.getDescription();
	}

	public String getSluggedTitle() {
		return information.getSluggedTitle();
	}

	public String getMarkedDescription() {
		return information.getMarkedDescription();
	}

	public String getSanitizedDescription() {
		return sanitize(information.getDescription());
	}

	public String getTagsAsString() {
		return information.getTagsAsString();
	}
	
	@Override
	public QuestionInformation getInformation() {
		return information;
	}
	
	public List<Tag> getTags() {
		return information.getTags();
	}
	
	public String getMostImportantTag(){
		List<Tag> tags = information.getTags();
		if(tags.isEmpty()){
			throw new IllegalStateException("a question must have at least one tag");
		}
		return tags.get(0).getName();
	}

	public UpdateStatus updateWith(QuestionInformation information) {
	    return new Updater().update(this, information);
	}
	
	protected void addHistory(Information newInformation) {
		this.history.add((QuestionInformation) newInformation);
	}

	public List<QuestionInformation> getHistory() {
		return history;
	}
	
	private void setInformation(QuestionInformation newInformation) {
		newInformation.setModeratable(this);
		if (this.information != null) {
			for (Tag tag : this.information.getTags()) {
				tag.decrementUsage();
			}
		}
		for (Tag tag : newInformation.getTags()) {
			tag.incrementUsage();
		}
        this.information = newInformation;
    }
	
	public DateTime getCreatedAt() {
		return createdAt;
	}

	@Override
	protected void updateApproved(Information approved) {
		QuestionInformation approvedQuestion = (QuestionInformation) approved;
		this.touchedBy(approvedQuestion.getAuthor());
		setInformation(approvedQuestion);
	}

	@Override
	public boolean isEdited() {
		return lastUpdatedAt.isAfter(createdAt);
	}
	
	public String getTypeName() {
        return getType().getSimpleName();
    }

    @Override
    public Class<? extends Votable> getType() {
        return Question.class;
    }
    
    public boolean wasMadeBy(User author) {
        return this.author.getId().equals(author.getId());
    }

	@Override
	public void add(Watcher watcher) {
		watchers.add(watcher);
	}

	@Override
	public void remove(Watcher watcher) {
		watchers.remove(watcher);
	}

	@Override
	public List<Watcher> getWatchers() {
		return watchers;
	}

	@Override
	public void add(Flag flag) {
		flags.add(flag);
	}
	
	@Override
	public boolean alreadyFlaggedBy(User user) {
		for (Flag flag : flags) {
			if (flag.createdBy(user))
				return true;
		}
		return false;
	}

	public boolean alreadyAnsweredBy(User user) {
		for (Answer answer : answers) {
			if(answer.getAuthor().getId() == user.getId()){
				return true;
			}
		}
		return false;
	}

	@Override
	public void remove() {
		this.moderationOptions.remove();
	}
	
	@Override
	public boolean isVisible() {
		return this.moderationOptions.isVisible();
	}
	
	@Override
	public boolean isVisibleForModeratorAndNotAuthor(User user) {
		return !this.isVisible() && user != null && !user.isAuthorOf(this);
	}
	
	public boolean isVisibleFor(User user) {
		return this.isVisible() || (user != null && (user.isModerator() || user.isAuthorOf(this)));
	}

	@Override
	public boolean hasPendingEdits() {
		for (QuestionInformation information : history) {
			if(information.isPending()) return true;
		}
		return false;
	}
	
	public void subtractAnswer(){
		answerCount--;
	}

	@Override
	public String getTypeNameKey() {
		return "question.type_name";
	}
	
    public String getTrimmedContent() {
        String markedDescription = getMarkedDescription();
        if (markedDescription.length() < 200)
            return markedDescription;
        return markedDescription.substring(0, 200) + "...";
    }
    
    public boolean hasAuthor(User user) {
		return user.getId().equals(author.getId());
    	
    }

	@Override
	public Question getQuestion() {
		return this;
	}

	public String getMetaDescription() {
		String title = getTitle();
		int index = title.length() < 15 ? title.length() : 14;
		String shortTitle = getTitle().substring(0, index);
		
		
		String markedDescription = getMarkedDescription();
		index = markedDescription.length() < 200 ? title.length() : 199;
		markedDescription = markedDescription.substring(0, index);
		return shortTitle + " " + markedDescription;
	}


}
