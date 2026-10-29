import { useState } from 'react';
import type { Dispatch } from 'react';
import { PART_KIND_INFO, FLAW_INFO } from '../game/data';
import { formatMoney } from '../game/rules';
import type { Action, GameState } from '../game/types';
import { Chip, Meter, Panel } from './ui';
import { condTone, partSpecs } from './format';

export function MarketTab({
  state,
  dispatch,
  onHaggle,
}: {
  state: GameState;
  dispatch: Dispatch<Action>;
  onHaggle: (id: string) => void;
}) {
  const [filter, setFilter] = useState<string>('all');
  const kinds = ['all', 'base', 'cpu', 'ram', 'storage', 'cooler', 'nic'];
  const lots = state.listings
    .filter((l) => filter === 'all' || l.part.kind === filter)
    .sort((a, b) => a.part.kind.localeCompare(b.part.kind) || a.askPrice - b.askPrice);

  return (
    <>
      <Panel
        title="Барахолка"
        icon="🛒"
        sub="Всё б/у, всё «работает». Проверяй перед покупкой — или узнаешь про дефекты уже после включения."
        right={
          <div className="row">
            <Chip tone="amber">Доставка на следующий день</Chip>
            <Chip tone={state.money > 3000 ? 'green' : 'red'}>{formatMoney(state.money)}</Chip>
          </div>
        }
      >
        <div className="row" style={{ marginBottom: 12 }}>
          {kinds.map((k) => (
            <button key={k} className={`btn btn-sm ${filter === k ? 'btn-amber' : ''}`} onClick={() => setFilter(k)}>
              {k === 'all' ? 'Всё' : `${PART_KIND_INFO[k as keyof typeof PART_KIND_INFO].icon} ${PART_KIND_INFO[k as keyof typeof PART_KIND_INFO].name}`}
            </button>
          ))}
        </div>

        {state.pending.length > 0 && (
          <div className="panel" style={{ padding: 10, marginBottom: 12, background: '#0d1822', borderColor: '#26455c' }}>
            <div className="tiny mono" style={{ color: 'var(--cyan)', marginBottom: 4 }}>
              🚚 В пути:
            </div>
            <div className="row">
              {state.pending.map((p, i) => (
                <Chip key={i} tone="cyan">
                  {p.part.name} — {p.arriveDay === state.day + 1 ? 'завтра' : `день ${p.arriveDay}`}
                </Chip>
              ))}
            </div>
          </div>
        )}

        {lots.length === 0 ? (
          <div className="empty">Тут пока пусто. Продавцы обновляют объявления каждый день.</div>
        ) : (
          <div className="grid-auto">
            {lots.map((lot) => {
              const p = lot.part;
              const kind = PART_KIND_INFO[p.kind];
              const knownFlaw = p.flawKnown && p.flaw !== 'none';
              return (
                <div className="lot" key={lot.id}>
                  <div className="spread">
                    <div className="row">
                      <span style={{ fontSize: 19 }}>{kind.icon}</span>
                      <div>
                        <div className="part-name">{p.name}</div>
                        <div className="part-meta">
                          {kind.name} · состояние {p.condition}%
                        </div>
                      </div>
                    </div>
                    <Chip tone={p.tier >= 3 ? 'violet' : p.tier >= 2 ? 'cyan' : undefined}>T{p.tier}</Chip>
                  </div>

                  <div className="row">
                    {partSpecs(p).slice(0, 4).map((sp, i) => (
                      <Chip key={i}>{sp}</Chip>
                    ))}
                  </div>

                  <Meter label="Состояние" right={`${p.condition}%`} value={p.condition} max={100} tone={condTone(p.condition)} />

                  <div className="seller">
                    <div className="avatar">{lot.seller.slice(0, 1).toUpperCase()}</div>
                    <div>
                      <div style={{ color: 'var(--text)', fontWeight: 650 }}>{lot.seller}</div>
                      <div style={{ fontSize: 10.5 }}>{lot.sellerTag}</div>
                    </div>
                  </div>

                  <div className="row">
                    {knownFlaw ? (
                      <Chip tone="red">
                        {FLAW_INFO[p.flaw].icon} {FLAW_INFO[p.flaw].name}
                      </Chip>
                    ) : lot.revealed ? (
                      <Chip tone="green">✅ Проверено продавцом</Chip>
                    ) : (
                      <Chip tone="amber">❓ дефекты не проверены</Chip>
                    )}
                    {lot.attempts > 0 && <Chip>торг: {lot.attempts}</Chip>}
                  </div>

                  {p.flavor && <div className="tiny dim" style={{ fontStyle: 'italic' }}>«{p.flavor}»</div>}

                  <div className="spread" style={{ marginTop: 'auto', paddingTop: 4 }}>
                    <div>
                      <div className="lot-price">{formatMoney(lot.askPrice)}</div>
                      {lot.askPrice < p.price && <div className="lot-old">{formatMoney(p.price)}</div>}
                    </div>
                    <div className="row">
                      <button
                        className="btn btn-sm"
                        title="Проверить товар перед покупкой: 150 ₽"
                        onClick={() => dispatch({ type: 'REVEAL_FLAW', listingId: lot.id })}
                        disabled={lot.revealed || state.money < 150}
                      >
                        🔎 150 ₽
                      </button>
                      <button className="btn btn-sm" onClick={() => onHaggle(lot.id)}>
                        💬 Торг
                      </button>
                      <button
                        className="btn btn-sm btn-primary"
                        onClick={() => dispatch({ type: 'BUY', listingId: lot.id })}
                        disabled={state.money < lot.askPrice}
                      >
                        Купить
                      </button>
                    </div>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </Panel>

      <Panel title="Инвентарь и советы" icon="📦">
        <div className="grid2">
          <div>
            <div className="tiny dim mono" style={{ marginBottom: 6 }}>НА СКЛАДЕ ({state.inventory.length})</div>
            {state.inventory.length === 0 ? (
              <div className="tiny dim">Пусто. Всё железо уже в машинах.</div>
            ) : (
              state.inventory.map((p) => (
                <div className="part-row" key={p.uid}>
                  <span>{PART_KIND_INFO[p.kind].icon}</span>
                  <div style={{ flex: 1 }}>
                    <div className="part-name">{p.name}</div>
                    <div className="part-meta">
                      {p.condition}% · {p.flawKnown ? (p.flaw === 'none' ? 'дефектов нет' : FLAW_INFO[p.flaw].name) : 'не проверено'}
                    </div>
                  </div>
                  <Chip tone="cyan">в сборке</Chip>
                </div>
              ))
            )}
          </div>
          <div className="tiny dim" style={{ display: 'flex', flexDirection: 'column', gap: 7 }}>
            <div>🔎 <b>Проверка (150 ₽)</b> — точное состояние и дефекты. Дешевле, чем купить хлам.</div>
            <div>💬 <b>Торг</b> — «Спросить» вытягивает правду, «Надавить» снижает цену, «Предложить» даёт шанс купить дешевле.</div>
            <div>🚚 <b>Доставка</b> — железо приходит на следующий день, так что планируй заранее.</div>
            <div>🧠 <b>Совет:</b> серверная ECC-память не встанет в десктопную плату, и наоборот.</div>
            <div>🗄️ <b>Совет:</b> стоечные серверы требуют шкаф 12U — купи его в «Путь → Апгрейды».</div>
          </div>
        </div>
      </Panel>
    </>
  );
}
