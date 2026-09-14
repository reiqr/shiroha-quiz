'use strict';

const assert = require('node:assert/strict');
const { analysis, FORMULA_ANALYSIS } = require('./fixtures.cjs');
const { SEL, snapshot, assertOnlyAnalysis, assertPayload, assertKeys } = require('./harness.cjs');

async function collapsed(h) {
  await h.page.locator(SEL.open).waitFor();
  const buttons = await h.page.locator(`${SEL.panel} button:visible`).evaluateAll(elements => elements.map(element => element.hasAttribute('data-ai-single-open-v613')));
  assert.deepEqual(buttons, [true], 'A collapsed panel must contain only the open button.');
  assert.equal(await h.page.locator(`${SEL.panel} ${SEL.text}:visible`).count(), 0);
  assert.equal(await h.page.locator(`${SEL.panel} ${SEL.chat}:visible`).count(), 0);
  assert.equal(await h.page.locator(`${SEL.input}:visible`).count(), 0);
}

async function saveAndCheck(h, expected) {
  const before = await snapshot(h.page);
  await h.page.locator(SEL.save).click();
  const after = await snapshot(h.page);
  assertOnlyAnalysis(before, after, expected);
  const stored = await h.page.evaluate(key => JSON.parse(localStorage.getItem(key)), h.keys.state);
  const question = stored.banks.find(bank => bank.id === after.bank.id).questions.find(question => question.id === after.question.id);
  assert.deepEqual(question, after.question, 'Saved draft did not reach localStorage.');
  await assertKeys(h.page, h.keys);
}

const cases = [
  {
    name: 'unconfigured-never-requests', options: { configured: false },
    async run(h) {
      await h.start();
      await h.quiet();
      await h.reveal();
      const open = h.page.locator(SEL.open);
      await open.waitFor();
      if (await open.isEnabled()) { await open.click(); await h.quiet(); }
      assert.equal(h.mock.calls.length, 0);
    },
  },
  {
    name: 'unconfigured-settings-visible-preserves-practice-answer-state', options: { configured: false },
    async run(h) {
      await h.start();
      await h.submitChoice('A');
      await h.jump(1); await h.submitChoice('B');
      const before = await snapshot(h.page);
      assert.equal(before.practice.idx, 1);
      assert.equal(before.practice.answered, 2);
      assert.deepEqual(before.practice.answerState[before.key].chosen, ['B']);
      assert(await h.page.locator('body').evaluate(body => body.classList.contains('practice-focus')));
      await h.page.locator(SEL.open).click();
      await h.quiet();
      await h.page.locator(`${SEL.panel} ${SEL.settings}`).click();
      await h.page.locator('#ai-settings.active').waitFor({ state: 'visible' });
      await h.page.locator('#ai-endpoint-v99').waitFor({ state: 'visible' });
      await h.page.locator('#ai-model-v99').waitFor({ state: 'visible' });
      assert.equal(await h.page.locator('body').evaluate(body => body.classList.contains('practice-focus')), false, 'AI settings remained hidden by practice-focus CSS.');
      assert.equal(await h.page.locator('.nav[data-view="ai-settings"].active').count(), 1);
      await h.quiet();
      const after = await snapshot(h.page);
      assert.deepEqual(after.identities, before.identities, 'Settings navigation replaced the practice/answerState objects.');
      assert.deepEqual(after.practice, before.practice, 'Settings navigation changed chosen answers, submission state, counters or position.');
      assert.deepEqual(after.progress, before.progress);
      assert.deepEqual(after.records, before.records, 'Opening settings finished or recorded the practice session.');
      assert.deepEqual(after.bank, before.bank);
      await h.page.locator('.nav[data-view="practice"]').click();
      await h.page.locator('#p-submit').waitFor({ state: 'visible' });
      const returned = await snapshot(h.page);
      assert.deepEqual(returned.identities, before.identities);
      assert.deepEqual(returned.practice, before.practice);
      assert.equal(returned.ui.progress, before.ui.progress);
      assert.equal(returned.ui.submitDisabled, true);
      assert.equal(returned.ui.revealDisabled, true);
      assert(await h.page.locator('#practice-card .option[data-key="B"] input').isChecked());
      assert.equal(h.mock.calls.length, 0, 'Unconfigured settings flow issued an API request.');
      await assertKeys(h.page, h.keys);
    },
  },
  {
    name: 'unanswered-and-collapsed-never-request',
    async run(h) {
      await h.start();
      assert.equal(await h.page.locator(`${SEL.panel} button`).count(), 0, 'Unanswered question exposes AI actions.');
      await h.page.locator('#p-submit').click();
      await h.quiet();
      assert.equal(await h.page.locator(`${SEL.panel} button`).count(), 0, 'Empty submission exposed AI actions.');
      await h.jump(1);
      await h.jump(0);
      await h.quiet();
      await h.reveal();
      await collapsed(h);
      await h.quiet();
      assert.equal(h.mock.calls.length, 0);
      await assertKeys(h.page, h.keys);
    },
  },
  {
    name: 'generate-follow-navigation-isolated-and-close',
    async run(h) {
      await h.start();
      await h.submitChoice('A');
      await collapsed(h);
      const before = await snapshot(h.page);
      const first = await h.generate(analysis('Q1_ANALYSIS_DRAFT'));
      assertPayload(first, 'SAFE_Q1', ['SAFE_Q2', 'SAFE_BLANK', 'SAFE_SHORT', 'SAFE_IMAGE']);
      assert.deepEqual(await snapshot(h.page), before, 'Generating must not persist or change practice.');
      const follow1 = await h.follow('Q1_FOLLOW_QUESTION', 'Q1_FOLLOW_REPLY');
      assertPayload(follow1, 'SAFE_Q1', ['SAFE_Q2']);
      assertPayload(follow1, 'Q1_ANALYSIS_DRAFT');
      await h.followInput();
      await h.page.locator(SEL.input).fill('Q1_UNSENT_DRAFT');

      await h.jump(1);
      assert.equal(await h.page.locator(`${SEL.panel} button`).count(), 0);
      await h.reveal();
      await collapsed(h);
      const second = await h.generate(analysis('Q2_ANALYSIS_DRAFT', { suggestedAnswer: 'B', confidence: 'MEDIUM' }));
      assertPayload(second, 'SAFE_Q2', ['SAFE_Q1', 'Q1_ANALYSIS_DRAFT', 'Q1_FOLLOW_QUESTION', 'Q1_FOLLOW_REPLY', 'Q1_UNSENT_DRAFT']);
      const follow2 = await h.follow('Q2_FOLLOW_QUESTION', 'Q2_FOLLOW_REPLY');
      assertPayload(follow2, 'SAFE_Q2', ['SAFE_Q1', 'Q1_FOLLOW_QUESTION', 'Q1_FOLLOW_REPLY']);
      assert.equal(await h.page.locator(`${SEL.panel} ${SEL.confidence}`).textContent(), '中等置信');

      await h.jump(0);
      await h.expandExisting();
      await h.text('Q1_ANALYSIS_DRAFT');
      assert((await h.page.locator(SEL.chat).textContent()).includes('Q1_FOLLOW_REPLY'));
      assert(!(await h.page.locator(SEL.panel).textContent()).includes('Q2_FOLLOW_REPLY'));
      await h.followInput();
      assert.equal(await h.page.locator(SEL.input).inputValue(), 'Q1_UNSENT_DRAFT');
      await h.page.locator(SEL.close).click();
      await collapsed(h);
      await h.quiet();
      await h.expandExisting();
      await h.text('Q1_ANALYSIS_DRAFT');
      assert.equal(h.mock.calls.length, 4);
    },
  },
  {
    name: 'inflight-question-switch-does-not-write-current-question', options: { ignoreAbort: true },
    async run(h) {
      await h.start(); await h.reveal();
      const original = await snapshot(h.page);
      const pending = h.mock.enqueue('analysis', analysis('DELAYED_Q1_DRAFT'), { hold: true });
      await h.page.locator(SEL.open).click();
      const call = await h.waitCall(pending);
      assertPayload(call, 'SAFE_Q1', ['SAFE_Q2']);
      await h.jump(1); await h.reveal();
      await h.generate(analysis('CURRENT_Q2_DRAFT'));
      await h.deliver(pending);
      await h.text('CURRENT_Q2_DRAFT');
      assert(!(await h.page.locator(SEL.panel).textContent()).includes('DELAYED_Q1_DRAFT'));
      const current = await snapshot(h.page);
      assert.equal(current.question.id, 'safe-single-2');
      assert.equal(current.question.analysis, original.bank.questions.find(question => question.id === current.question.id).analysis);
    },
  },
  {
    name: 'new-session-clears-analysis-conversation-and-draft',
    async run(h) {
      await h.start(); await h.reveal();
      await h.generate(analysis('OLD_SESSION_ANALYSIS'));
      await h.follow('OLD_SESSION_QUESTION', 'OLD_SESSION_REPLY');
      await h.followInput();
      await h.page.locator(SEL.input).fill('OLD_SESSION_UNSENT');
      await h.page.locator('#p-exit').click();
      await h.start(); await h.reveal();
      await collapsed(h);
      await h.quiet();
      const call = await h.generate(analysis('NEW_SESSION_ANALYSIS'));
      assertPayload(call, 'SAFE_Q1', ['OLD_SESSION_ANALYSIS', 'OLD_SESSION_QUESTION', 'OLD_SESSION_REPLY', 'OLD_SESSION_UNSENT']);
      assert.equal(await h.page.locator(`${SEL.chat} .practice-ai-msg-v613`).count(), 0);
      await h.followInput();
      assert.equal(await h.page.locator(SEL.input).inputValue(), '');
    },
  },
  ...['exit', 'reset', 'restart'].map(mode => ({
    name: `late-analysis-cannot-write-new-session-${mode}`, options: { ignoreAbort: true },
    async run(h) {
      await h.start(); await h.reveal();
      const pending = h.mock.enqueue('analysis', analysis('STALE_OLD_SESSION_RESULT'), { hold: true });
      await h.page.locator(SEL.open).click(); await h.waitCall(pending);
      if (mode === 'exit') { await h.page.locator('#p-exit').click(); await h.start(); }
      else {
        // The focus layout hides setup controls; reveal layout only, then use the real handlers.
        await h.page.evaluate(() => document.body.classList.remove('practice-focus'));
        if (mode === 'reset') await h.page.locator('#reset-practice-btn').click();
        await h.page.locator('#start-practice-btn').click();
      }
      await h.reveal();
      await collapsed(h);
      await h.generate(analysis('FRESH_NEW_SESSION_RESULT'));
      const before = await snapshot(h.page);
      await h.deliver(pending);
      await h.text('FRESH_NEW_SESSION_RESULT');
      assert(!(await h.page.locator(SEL.panel).textContent()).includes('STALE_OLD_SESSION_RESULT'));
      assert.deepEqual(await snapshot(h.page), before);
    },
  })),
  {
    name: 'late-follow-up-cannot-write-new-session', options: { ignoreAbort: true },
    async run(h) {
      await h.start(); await h.reveal();
      await h.generate(analysis('OLD_FOLLOW_SESSION_ANALYSIS'));
      await h.followInput(); await h.page.locator(SEL.input).fill('OLD_PENDING_FOLLOW');
      const pending = h.mock.enqueue('follow', 'STALE_FOLLOW_REPLY', { hold: true });
      await h.page.locator(SEL.send).click(); await h.waitCall(pending);
      await h.page.locator('#p-exit').click(); await h.start(); await h.reveal();
      await h.generate(analysis('FRESH_FOLLOW_SESSION_ANALYSIS'));
      await h.deliver(pending);
      await h.text('FRESH_FOLLOW_SESSION_ANALYSIS');
      assert(!(await h.page.locator(SEL.panel).textContent()).includes('STALE_FOLLOW_REPLY'));
      const fresh = await h.follow('FRESH_FOLLOW_QUESTION', 'FRESH_FOLLOW_REPLY');
      assertPayload(fresh, 'SAFE_Q1', ['OLD_PENDING_FOLLOW', 'STALE_FOLLOW_REPLY', 'OLD_FOLLOW_SESSION_ANALYSIS']);
    },
  },
  {
    name: 'stop-and-failure-preserve-analysis-history-and-follow-draft',
    async run(h) {
      await h.start(); await h.reveal();
      await h.generate(analysis('PRESERVED_ANALYSIS'));
      await h.follow('PRESERVED_QUESTION', 'PRESERVED_REPLY');
      const history = await h.page.locator(SEL.chat).textContent();
      await h.followInput();
      await h.page.locator(SEL.input).fill('CANCELLED_FOLLOW_DRAFT');
      const pending = h.mock.enqueue('follow', 'MUST_NOT_APPEAR_AFTER_STOP', { hold: true });
      await h.page.locator(SEL.send).click(); await h.waitCall(pending);
      await h.page.locator(SEL.stop).click();
      await h.idle();
      pending.release();
      await h.page.waitForTimeout(150);
      await h.followInput();
      assert.equal(await h.page.locator(SEL.input).inputValue(), 'CANCELLED_FOLLOW_DRAFT');
      assert.equal(await h.page.locator(SEL.chat).textContent(), history);
      await h.text('PRESERVED_ANALYSIS');

      await h.page.locator(SEL.input).fill('FAILED_FOLLOW_DRAFT');
      const failure = h.mock.enqueue('follow', '', { status: 503 });
      await h.page.locator(SEL.send).click(); await h.waitCall(failure); await h.idle();
      await h.followInput();
      assert.equal(await h.page.locator(SEL.input).inputValue(), 'FAILED_FOLLOW_DRAFT');
      assert.equal(await h.page.locator(SEL.chat).textContent(), history);
      assert(!(await h.page.locator(SEL.panel).textContent()).includes('MUST_NOT_APPEAR_AFTER_STOP'));
      await h.text('PRESERVED_ANALYSIS');
    },
  },
  {
    name: 'failed-or-cancelled-regeneration-keeps-history',
    async run(h) {
      await h.start(); await h.reveal();
      await h.generate(analysis('EXISTING_ANALYSIS'));
      await h.follow('EXISTING_FOLLOW_QUESTION', 'EXISTING_FOLLOW_REPLY');
      await h.followInput(); await h.page.locator(SEL.input).fill('EXISTING_UNSENT_DRAFT');
      const history = await h.page.locator(SEL.chat).textContent();
      const pending = h.mock.enqueue('analysis', analysis('CANCELLED_REGENERATION'), { hold: true });
      await h.page.locator(SEL.regenerate).click(); await h.waitCall(pending);
      assert.equal(await h.page.locator(SEL.chat).textContent(), history, 'Regeneration cleared history before success.');
      await h.page.locator(SEL.stop).click(); await h.idle(); pending.release();
      await h.text('EXISTING_ANALYSIS');
      assert.equal(await h.page.locator(SEL.chat).textContent(), history);
      await h.generate(analysis('IGNORED_HTTP_ERROR'), { regenerate: true, status: 503 });
      await h.text('EXISTING_ANALYSIS');
      assert.equal(await h.page.locator(SEL.chat).textContent(), history);
      await h.followInput();
      assert.equal(await h.page.locator(SEL.input).inputValue(), 'EXISTING_UNSENT_DRAFT');
    },
  },
  {
    name: 'malformed-regeneration-retains-draft-valid-success-resets-conversation',
    async run(h) {
      await h.start(); await h.reveal();
      await h.generate(analysis('VALID_OLD_ANALYSIS'));
      await h.follow('VALID_OLD_QUESTION', 'VALID_OLD_REPLY');
      await h.followInput(); await h.page.locator(SEL.input).fill('VALID_OLD_UNSENT');
      const history = await h.page.locator(SEL.chat).textContent();
      const malformed = h.mock.enqueue('analysis', 'not JSON');
      await h.page.locator(SEL.regenerate).click(); await h.waitCall(malformed); await h.idle();
      await h.text('VALID_OLD_ANALYSIS');
      assert.equal(await h.page.locator(SEL.chat).textContent(), history);
      await h.followInput(); assert.equal(await h.page.locator(SEL.input).inputValue(), 'VALID_OLD_UNSENT');
      const success = h.mock.enqueue('analysis', analysis('VALID_NEW_ANALYSIS'), { hold: true });
      await h.page.locator(SEL.regenerate).click(); await h.waitCall(success);
      assert.equal(await h.page.locator(SEL.chat).textContent(), history);
      success.release(); await h.text('VALID_NEW_ANALYSIS');
      assert.equal(await h.page.locator(`${SEL.chat} .practice-ai-msg-v613`).count(), 0);
      await h.followInput(); assert.equal(await h.page.locator(SEL.input).inputValue(), '');
      const fresh = await h.follow('AFTER_REGENERATION_QUESTION', 'AFTER_REGENERATION_REPLY');
      assertPayload(fresh, 'VALID_NEW_ANALYSIS', ['VALID_OLD_QUESTION', 'VALID_OLD_REPLY', 'VALID_OLD_UNSENT']);
    },
  },
  {
    name: 'initial-failure-and-stop-never-persist-can-retry',
    async run(h) {
      await h.start(); await h.reveal();
      const before = await snapshot(h.page);
      const failure = h.mock.enqueue('analysis', '', { status: 503 });
      await h.page.locator(SEL.open).click(); await h.waitCall(failure); await h.idle();
      assert.deepEqual(await snapshot(h.page), before);
      const pending = h.mock.enqueue('analysis', analysis('STOPPED_INITIAL_ANALYSIS'), { hold: true });
      await h.page.locator(SEL.regenerate).click(); await h.waitCall(pending);
      await h.page.locator(SEL.stop).click(); await h.idle(); pending.release();
      assert.deepEqual(await snapshot(h.page), before);
      await h.generate(analysis('RECOVERED_INITIAL_ANALYSIS'), { regenerate: true });
      assert.deepEqual(await snapshot(h.page), before);
    },
  },
  ...['answered', 'revealed'].map(mode => ({
    name: `save-only-analysis-preserves-${mode}-practice`,
    async run(h) {
      await h.start();
      if (mode === 'answered') await h.submitChoice('B'); else await h.reveal();
      const before = await snapshot(h.page);
      await h.generate(analysis('ONLY_THIS_ANALYSIS_IS_SAVED', { suggestedAnswer: 'B', matchesLocalAnswer: false, needsReview: true, warning: 'Synthetic disagreement, never replace local answer.' }));
      await h.follow('UNSAVED_FOLLOW_QUESTION', 'UNSAVED_FOLLOW_REPLY');
      assert.deepEqual(await snapshot(h.page), before);
      await saveAndCheck(h, 'ONLY_THIS_ANALYSIS_IS_SAVED');
      const raw = await h.page.evaluate(key => localStorage.getItem(key), h.keys.state);
      assert(!raw.includes('UNSAVED_FOLLOW_QUESTION') && !raw.includes('UNSAVED_FOLLOW_REPLY'));
    },
  })),
  {
    name: 'declined-overwrite-preserves-local-analysis-and-ai-draft',
    async run(h) {
      await h.start(); await h.reveal();
      await h.generate(analysis('OVERWRITE_DRAFT'));
      const before = await snapshot(h.page);
      const raw = await h.page.evaluate(key => localStorage.getItem(key), h.keys.state);
      let confirmationSeen = false;
      h.page.removeAllListeners('dialog');
      h.page.on('dialog', dialog => { confirmationSeen = dialog.type() === 'confirm'; return dialog.dismiss(); });
      await h.page.locator(SEL.save).click();
      assert.equal(confirmationSeen, true, 'Existing local analysis was overwritten without confirmation.');
      assert.deepEqual(await snapshot(h.page), before);
      assert.equal(await h.page.evaluate(key => localStorage.getItem(key), h.keys.state), raw);
      await h.text('OVERWRITE_DRAFT');
      assert(await h.page.locator(SEL.save).isEnabled());
      h.page.removeAllListeners('dialog');
      h.page.on('dialog', dialog => dialog.accept());
      await saveAndCheck(h, 'OVERWRITE_DRAFT');
    },
  },
  {
    name: 'rich-analysis-plain-save-removes-old-rich-field-preserves-stem-options', options: { rich: true },
    async run(h) {
      await h.start(); await h.reveal();
      const before = await snapshot(h.page);
      assert(before.question.richContent.fields.analysis.text.includes('OLD_RICH_ANALYSIS'));
      assert(before.question.richContent.features.includes('docx_table'));
      await h.generate(analysis('PLAIN_ANALYSIS_REPLACES_OLD_TABLE'));
      await saveAndCheck(h, 'PLAIN_ANALYSIS_REPLACES_OLD_TABLE');
      const after = await snapshot(h.page);
      assert.equal(after.question.richContent.fields.analysis, undefined, 'Plain analysis left stale richContent.fields.analysis.');
      assert(!after.question.richContent.features.includes('docx_table'));
      assert(after.question.richContent.features.includes('latex_formula'));
      assert.deepEqual(after.question.richContent.fields.question, before.question.richContent.fields.question);
      assert.deepEqual(after.question.richContent.fields.options, before.question.richContent.fields.options);
      const raw = await h.page.evaluate(key => localStorage.getItem(key), h.keys.state);
      const saved = JSON.parse(raw).banks[0].questions[0];
      assert(!JSON.stringify(saved).includes('OLD_RICH_ANALYSIS'));
      assert(raw.includes('OLD_RICH_ANALYSIS'), 'Historical record was unexpectedly rewritten.');
    },
  },
  {
    name: 'rich-analysis-formula-save-keeps-backslashes-and-updates-only-analysis', options: { rich: true },
    async run(h) {
      await h.start(); await h.submitChoice('A');
      const before = await snapshot(h.page);
      await h.generate(analysis(FORMULA_ANALYSIS), { visibleText: 'FORMULA_SAVE_MARKER' });
      assert.deepEqual(await snapshot(h.page), before, 'Formula draft was persisted before confirmation.');
      await saveAndCheck(h, FORMULA_ANALYSIS);
      const after = await snapshot(h.page);
      assert.equal(after.question.richContent.fields.analysis.text, FORMULA_ANALYSIS);
      assert(after.question.richContent.fields.analysis.features.includes('latex_formula'));
      assert(!after.question.richContent.features.includes('docx_table'));
      assert.deepEqual(after.question.richContent.fields.question, before.question.richContent.fields.question);
      assert.deepEqual(after.question.richContent.fields.options, before.question.richContent.fields.options);
      const roundTrip = JSON.parse(JSON.stringify(after.question));
      for (const command of [String.raw`\frac`, String.raw`\sqrt`, String.raw`\alpha`, String.raw`\beta`, String.raw`\gamma`]) {
        assert(roundTrip.analysis.includes(command), `Formula command was corrupted: ${command}`);
        assert(roundTrip.richContent.fields.analysis.text.includes(command));
      }
      await h.jump(1); await h.jump(0); await h.expandExisting();
      const current = await snapshot(h.page);
      assert.equal(current.question.analysis, FORMULA_ANALYSIS);
      assert.equal(current.practice.answered, 1);
      const call = await h.follow('FORMULA_FOLLOW_QUESTION', 'FORMULA_FOLLOW_REPLY');
      assert(call.body.messages.some(message => message.role === 'assistant' && message.content === FORMULA_ANALYSIS), 'Saved formula/rich field invalidated or corrupted the existing AI session.');
      assertPayload(call, 'SAFE_Q1', ['SAFE_Q2']);
    },
  },
  {
    name: 'quota-save-rolls-back-memory-storage-and-preserves-draft',
    async run(h) {
      await h.start(); await h.submitChoice('A');
      await h.generate(analysis('QUOTA_DRAFT_TO_RETRY'));
      await h.follow('QUOTA_UNSAVED_QUESTION', 'QUOTA_UNSAVED_REPLY');
      const history = await h.page.locator(SEL.chat).textContent();
      const before = await snapshot(h.page);
      const stored = await h.page.evaluate(key => localStorage.getItem(key), h.keys.state);
      await h.page.evaluate(key => {
        const original = Storage.prototype.setItem;
        window.__webAiQuotaTest = { attempts: 0, original };
        Storage.prototype.setItem = function (name, value) {
          if (this === localStorage && String(name) === key) {
            window.__webAiQuotaTest.attempts++;
            throw new DOMException('Synthetic quota exceeded', 'QuotaExceededError');
          }
          return original.call(this, name, value);
        };
      }, h.keys.state);
      try {
        await h.page.locator(SEL.save).click();
        assert((await h.page.evaluate(() => window.__webAiQuotaTest.attempts)) > 0, 'Quota injection did not reach the save path.');
        assert.deepEqual(await snapshot(h.page), before, 'Quota failure did not roll back in-memory question/bank/practice.');
        assert.equal(await h.page.evaluate(key => localStorage.getItem(key), h.keys.state), stored);
        await h.text('QUOTA_DRAFT_TO_RETRY');
        assert.equal(await h.page.locator(SEL.chat).textContent(), history);
        assert(await h.page.locator(SEL.save).isEnabled(), 'Quota failure incorrectly marked the draft saved.');
      } finally {
        await h.page.evaluate(() => { Storage.prototype.setItem = window.__webAiQuotaTest.original; delete window.__webAiQuotaTest; });
      }
      await saveAndCheck(h, 'QUOTA_DRAFT_TO_RETRY');
      assert.equal(h.pageErrors.length, 0, 'Quota exception escaped the save handler.');
    },
  },
  {
    name: 'unsent-images-force-insufficient-information-even-if-model-confident',
    async run(h) {
      await h.start(); await h.jump(4); await h.reveal();
      assert((await h.page.locator('#practice-card img.question-image').count()) > 0, 'Image fixture was not rendered.');
      const call = await h.generate(analysis('MODEL_CLAIMS_HIGH_CONFIDENCE', { confidence: 'HIGH', needsReview: false }));
      assertPayload(call, 'SAFE_IMAGE', ['SAFE_Q1', 'SAFE_Q2']);
      const messages = JSON.stringify(call.body.messages);
      assert(!messages.includes('data:image/') && !messages.includes('image_url'), 'Text-only AI request transmitted image bytes.');
      assert.equal(await h.page.locator(SEL.confidence).textContent(), '信息不足');
      const panel = await h.page.locator(SEL.panel).textContent();
      assert(/图片|图像/.test(panel) && /信息不足|未发送|无法|人工/.test(panel), 'Missing explicit warning about unsent image information.');
      const follow = await h.follow('Can you actually see the picture?', 'Mock cannot see a picture.');
      assert(!JSON.stringify(follow.body.messages).includes('data:image/'));
    },
  },
  {
    name: 'multi-blank-submit-equivalents-ai-save-does-not-change-answer',
    async run(h) {
      await h.start(); await h.jump(2);
      const inputs = h.page.locator('#practice-card .multi-blank-answer-input-v58914');
      assert.equal(await inputs.count(), 2);
      await inputs.nth(0).fill('one'); await inputs.nth(1).fill('two');
      await h.quiet();
      await h.page.locator('#p-submit').click();
      const submitted = await snapshot(h.page);
      assert.deepEqual(submitted.practice.answerState[submitted.key].chosen, ['one', 'two']);
      assert.equal(submitted.practice.answerState[submitted.key].answered, true);
      assert.equal(submitted.practice.answerState[submitted.key].correct, true);
      assert.equal(submitted.practice.answered, 1);
      assert.deepEqual(submitted.question.blankAnswers, [['1', 'one'], ['2', 'two']]);
      const call = await h.generate(analysis('BLANK_ANALYSIS_ONLY', { suggestedAnswer: '999;999' }));
      assertPayload(call, 'SAFE_BLANK', ['SAFE_Q1', 'SAFE_Q2']);
      assert(JSON.stringify(call.body.messages).includes('blankAnswers'));
      await saveAndCheck(h, 'BLANK_ANALYSIS_ONLY');
      await h.page.locator('#p-submit').evaluate(button => button.click());
      assert.equal((await snapshot(h.page)).practice.answered, 1, 'AI save enabled a duplicate submission.');
    },
  },
  ...['right', 'wrong', 'reveal'].map(mode => ({
    name: `short-answer-${mode}-path-unaffected-by-ai`,
    async run(h) {
      await h.start(); await h.jump(3);
      await h.page.locator('#practice-card .text-answer').fill('SAFE_USER_SHORT_ANSWER');
      if (mode === 'reveal') await h.reveal(); else await h.page.locator('#p-submit').click();
      const pending = await snapshot(h.page);
      assert.equal(pending.practice.answered, 0, 'Short answer was scored before self-assessment.');
      assert.deepEqual(pending.practice.answerState[pending.key].chosen, ['SAFE_USER_SHORT_ANSWER']);
      await h.page.locator('#p-self-right').waitFor();
      await h.page.locator('#p-self-wrong').waitFor();
      const call = await h.generate(analysis('SHORT_ANALYSIS_ONLY', { suggestedAnswer: 'Do not overwrite the short reference.' }));
      assertPayload(call, 'SAFE_SHORT', ['SAFE_Q1', 'SAFE_Q2']);
      await saveAndCheck(h, 'SHORT_ANALYSIS_ONLY');
      const before = await snapshot(h.page);
      assert.equal(await h.page.locator('#p-self-right').count(), 1, 'AI save removed short-answer self-assessment controls.');
      await h.page.locator(mode === 'wrong' ? '#p-self-wrong' : '#p-self-right').click();
      const scored = await snapshot(h.page);
      assert.equal(scored.practice.answered, 1);
      assert.equal(scored.practice.answerState[scored.key].answered, true);
      assert.equal(scored.practice.answerState[scored.key].correct, mode !== 'wrong');
      assert.deepEqual(scored.practice.answerState[scored.key].chosen, ['SAFE_USER_SHORT_ANSWER']);
      assert.deepEqual(scored.question.answer, before.question.answer);
      assert.equal(scored.practice.idx, 3);
    },
  })),
];

module.exports = { cases, collapsed };
