import type { Dispatch } from 'react';
import { FLAW_INFO } from '../game/data';
import { formatMoney } from '../game/rules';
import type { Action, GameState } from '../game/types';
import { Modal, Chip } from './ui';

export function HaggleModal({
  state,
  dispatch,
  listingId,
  onClose,
}: {
  state: GameState;
  dispatch: Dispatch<Action>;
  listingId: string;
  onClose: () => void;
}) {
  const lot = state.listings.find((l) => l.id === listingId);
  if (!lot) return null;
  const p = lot.part;
  const canOffer = p.flawKnown ? Math.round((lot.askPrice * 0.72) / 10) * 10 : Math.round((lot.askPrice * 0.7) / 10) * 10;

  return (
    <Modal onClose={onClose}>
      <div className="modal-title" style={{ marginBottom: 4 }}>
        <span className="big">💬</span> Торг: {p.name}
      </div>
      <div className="tiny dim mono" style={{ marginBottom: 10 }}>
        продавец {lot.seller} · {lot.sellerTag}
      </div>

      <div className="row" style={{ marginBottom: 10 }}>
        <Chip tone="green">Цена: {formatMoney(lot.askPrice)}</Chip>
        <Chip>Рынок: {formatMoney(p.price)}</Chip>
        <Chip tone="cyan">Попыток торга: {lot.attempts}</Chip>
        {p.flawKnown && p.flaw !== 'none' ? (
          <Chip tone="red">⚠ {FLAW_INFO[p.flaw].name}</Chip>
        ) : (
          <Chip tone="amber">❓ дефекты неизвестны</Chip>
        )}
      </div>

      <div className="chat">
        {lot.chat.map((m, i) => (
          <div key={i} className={`bubble ${m.from}`}>
            {m.text}
          </div>
        ))}
      </div>

      <div className="grid2" style={{ marginTop: 12 }}>
        <button className="btn" onClick={() => dispatch({ type: 'HAGGLE', listingId, line: 'ask' })} disabled={lot.revealed}>
          🙋 Спросить о дефектах
        </button>
        <button className="btn" onClick={() => dispatch({ type: 'HAGGLE', listingId, line: 'pressure' })}>
          🔍 Надавить (навык железа)
        </button>
        <button className="btn btn-amber" onClick={() => dispatch({ type: 'HAGGLE', listingId, line: 'offer' })}>
          🪙 Предложить ≈ {formatMoney(canOffer)}
        </button>
        <button
          className="btn btn-primary"
          onClick={() => {
            dispatch({ type: 'BUY', listingId });
            onClose();
          }}
          disabled={state.money < lot.askPrice}
        >
          🤝 Взять за {formatMoney(lot.askPrice)}
        </button>
      </div>

      <div className="tiny dim" style={{ marginTop: 10 }}>
        Чем выше навык железа и репутация, тем охотнее продавцы верят, что ты видишь дефекты. Слишком много попыток — и продавец
        обидится и поднимет цену.
      </div>
    </Modal>
  );
}
