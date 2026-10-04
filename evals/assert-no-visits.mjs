/**
 * Checks the answer to "Which pets have not visited the clinic yet?".
 *
 * Max and Samantha are the two pets with visits, so two answers are right: a list
 * of the eleven other pets, or a sentence such as "every pet except Max and Samantha".
 * Two of the eleven are called Lucky, so the list names Lucky twice, or says there
 * are two.
 *
 * Many answers add "Only Max and Samantha have been to the clinic" before or after
 * the list. Max and Samantha may therefore appear outside the list, where the text on
 * that side talks about visits. The list runs from the first to the last mention of
 * the eleven pets, so a note about the two Luckys after that sentence pulls it into
 * the list. Inside the list, Max and Samantha may therefore appear in a sentence of
 * their own about visits, one that is not a list item and does not name any of the eleven.
 * Anywhere else inside the list, the answer counts them among the pets without visits.
 */
const OTHERS = ['Leo', 'Basil', 'Rosy', 'Jewel', 'Iggy', 'George', 'Mulligan', 'Freddy', 'Sly'];
const EXCEPTION = /\b(except|other than|apart from|besides|but not|excluding)\b[^.\n]*\b(Max|Samantha)\b/i;
const ABOUT_VISITS = /visit|been to|been in|seen/i;
// a bullet, a numbered item or a table row
const LIST_ITEM = /^\s*([-*+•]|\d+[.)]|\|)/;

const mentions = (text, name) => new RegExp(`\\b${name}\\b`).test(text);
const positions = (text, name) => [...text.matchAll(new RegExp(`\\b${name}\\b`, 'g'))].map((match) => match.index);

function lineAt(text, at) {
  const end = text.indexOf('\n', at);
  return text.slice(text.lastIndexOf('\n', at) + 1, end < 0 ? text.length : end);
}

function sentenceAt(text, at) {
  const before = text.slice(0, at);
  const start = Math.max(before.lastIndexOf('\n'), ...['. ', '! ', '? '].map((end) => before.lastIndexOf(end))) + 1;
  const length = text.slice(at).search(/[.!?](\s|$)|\n/);
  return text.slice(start, length < 0 ? text.length : at + length);
}

// "The only pets with a visit on record are Samantha and Max."
function inSentenceAboutVisits(text, at) {
  const sentence = sentenceAt(text, at);
  return (
    !LIST_ITEM.test(lineAt(text, at)) &&
    ABOUT_VISITS.test(sentence) &&
    ![...OTHERS, 'Lucky'].some((name) => mentions(sentence, name))
  );
}

export default function (output) {
  const text = String(output ?? '');
  if (EXCEPTION.test(text)) {
    return { pass: true, score: 1, reason: 'names Max and Samantha as the exception' };
  }

  const missing = OTHERS.filter((name) => !mentions(text, name));
  const luckyTwice =
    positions(text, 'Lucky').length >= 2 ||
    /\b(two|both)\b[^.\n]*\bLucky\b|\bLucky\b[^.\n]*\b(two|both)\b/i.test(text);
  if (!luckyTwice) missing.push('the second Lucky');

  // the list runs from the first to the last mention of the eleven pets
  const listed = [...OTHERS, 'Lucky'].flatMap((name) => positions(text, name));
  const start = Math.min(...listed);
  const end = Math.max(...listed);
  const counted = ['Max', 'Samantha'].filter((name) =>
    positions(text, name).some(
      (at) =>
        (at > start && at < end && !inSentenceAboutVisits(text, at)) ||
        (at > end && !ABOUT_VISITS.test(text.slice(end))) ||
        (at < start && !ABOUT_VISITS.test(text.slice(0, start))),
    ),
  );

  if (missing.length === 0 && counted.length === 0) {
    return { pass: true, score: 1, reason: 'lists the eleven pets without visits' };
  }
  const reasons = [];
  if (missing.length) reasons.push(`leaves out ${missing.join(', ')}`);
  if (counted.length) reasons.push(`counts ${counted.join(' and ')}, which ${counted.length > 1 ? 'have' : 'has'} visits`);
  return { pass: false, score: 0, reason: `The answer ${reasons.join(' and ')}.` };
}
