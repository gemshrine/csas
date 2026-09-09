package com.dev.csas.course;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class Course {

    private final List<StudentID> studentIDList;
    private final List<Work> works = new ArrayList<>();
    private final List<Score> scores = new ArrayList<>();

    public Course(List<StudentID> studentIDList) {
        this.studentIDList = List.copyOf(studentIDList);
    }

    public long addWork(String title, int maxScore, LocalDate date) {

        validateWorkMaxScore(maxScore);
        validateWorkDate(date);

        long number = works.stream().mapToLong(Work::getNumber).max().orElse(0) + 1;
        Work work = new Work(number, title, maxScore, date);
        works.add(work);

        return number;
    }

    public void score(long workNumber, StudentID studentID, int earnedScore) {

        validateStudentExists(studentID);

        Work work = findWork(workNumber);
        validateScore(earnedScore, work);

        Score score = getScoreByWorkNumberAndStudentID(workNumber, studentID);

        if (score != null) {
            score.setScore(earnedScore);
            return;
        }

        Score newScore = new Score(workNumber, studentID, earnedScore);
        scores.add(newScore);
    }

    public double rating(StudentID studentID) {

        validateStudentExists(studentID);

        double max = works.stream()
                .mapToDouble(Work::getMaxScore)
                .sum();

        double studentSum = scores.stream()
                .filter(s -> s.getStudentId().equals(studentID))
                .mapToDouble(Score::getScore)
                .sum();

        return max != 0 ? studentSum * 100.0 / max : 100.0 ;
    }

    public void updateWorkMaxScore(long workNumber, int newMax) {

        validateWorkMaxScore(newMax);

        Work work = findWork(workNumber);
        work.updateMaxScore(newMax);
    }

    public void updateWorkTitle(long workNumber, String newTitle) {
        Work work = findWork(workNumber);
        work.updateTitle(newTitle);
    }

    public void updateWorkDate(long workNumber, LocalDate newDate) {
        validateWorkDate(newDate);

        Work work = findWork(workNumber);
        work.updateDate(newDate);
    }

    private Score getScoreByWorkNumberAndStudentID(long workNumber, StudentID studentID) {
        return scores.stream()
                .filter(score -> score.getStudentId().equals(studentID))
                .filter(score -> score.getWorkNumber() == workNumber)
                .findFirst()
                .orElse(null);
    }

    public int getScore(long workNumber, StudentID studentID) {
        Score score = getScoreByWorkNumberAndStudentID(workNumber, studentID);
        if (score == null) {
            throw new ScoreNotFoundException("Оценка не найдена");
        }
        return score.getScore();
    }

    private boolean isStudentExists(StudentID student) {
        return studentIDList.stream()
                .anyMatch(id -> id.id() == student.id());
    }

    private void validateStudentExists(StudentID studentID) {
        if (!isStudentExists(studentID)) {
            throw new NonExistentStudentException(String.format("Указанного студента нет на данном курсе: %s", studentID.id()));
        }
    }

    private void validateWorkMaxScore(int maxScore) {
        if (maxScore <= 0) {
            throw new InvalidWorkMaxScoreException("Нельзя создать работу с количеством максимальных баллов равным 0");
        }
    }

    private void validateWorkDate(LocalDate date) {
        if (date == null) {
            throw new InvalidWorkDateException("Нельзя создать работу с пустой датой");
        }
    }

    private Work findWork(long workNumber) {
        return works.stream()
                .filter(w -> w.getNumber() == workNumber)
                .findFirst()
                .orElseThrow(() -> new WorkNotFoundException(String.format("Работа не найдена: %s", workNumber)));
    }

    private void validateScore(int score, Work work) {
        if (score > work.getMaxScore()) {
            throw new InvalidScoreValueException(
                    String.format("Оценка %s превышает максимальный балл %s", score, work.getMaxScore())
            );
        }
        if (score < 0) {
            throw new InvalidScoreValueException(String.format("Оценка не может быть отрицательной: %s", score));
        }
    }

    public LocalDate getWorkDate(long workNumber) {
        return findWork(workNumber).getDate();
    }
}
