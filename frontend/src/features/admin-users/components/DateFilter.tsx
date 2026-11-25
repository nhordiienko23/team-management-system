import { useId, useLayoutEffect, useRef, useState, type KeyboardEvent } from "react";
import "./DateFilter.css";

export function toBackendDate(value: string): string | null {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(value)) return null;
  const [yearText, monthText, dayText] = value.split("-");
  const year = Number(yearText);
  const month = Number(monthText);
  const day = Number(dayText);
  const date = new Date(Date.UTC(year, month - 1, day));
  if (
    date.getUTCFullYear() !== year ||
    date.getUTCMonth() !== month - 1 ||
    date.getUTCDate() !== day
  ) {
    return null;
  }
  return `${dayText}.${monthText}.${yearText}`;
}

function isoDate(date: Date): string {
  return `${String(date.getFullYear()).padStart(4, "0")}-${String(date.getMonth() + 1).padStart(2, "0")}-${String(date.getDate()).padStart(2, "0")}`;
}

function parseDisplayDate(value: string): string | null {
  const match = /^(\d{2})\.(\d{2})\.(\d{4})$/.exec(value);
  if (!match) return null;
  const iso = `${match[3]}-${match[2]}-${match[1]}`;
  return toBackendDate(iso) ? iso : null;
}

function dateFromIso(value: string): Date {
  const [year, month, day] = value.split("-").map(Number);
  return new Date(year, month - 1, day);
}

function moveMonths(date: Date, amount: number): Date {
  const target = new Date(date.getFullYear(), date.getMonth() + amount, 1);
  if (target.getFullYear() < 100 || target.getFullYear() > 9999) return date;
  target.setDate(Math.min(date.getDate(), new Date(target.getFullYear(), target.getMonth() + 1, 0).getDate()));
  return target;
}

const monthFormat = new Intl.DateTimeFormat("en-US", { month: "long", year: "numeric" });
const dayFormat = new Intl.DateTimeFormat("en-US", { weekday: "long", month: "long", day: "numeric", year: "numeric" });
const weekdays = ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"];

export function DateFilter({ name, label, defaultValue }: {
  name: string;
  label: string;
  defaultValue: string;
}) {
  const id = useId();
  const [text, setText] = useState(() => toBackendDate(defaultValue) ?? "");
  const [open, setOpen] = useState(false);
  const [focusedDate, setFocusedDate] = useState(() =>
    toBackendDate(defaultValue) ? dateFromIso(defaultValue) : new Date(),
  );
  const inputRef = useRef<HTMLInputElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);
  const dayRef = useRef<HTMLButtonElement>(null);
  const focusDay = useRef(false);
  const selected = parseDisplayDate(text);

  useLayoutEffect(() => {
    if (open && focusDay.current) {
      dayRef.current?.focus();
      focusDay.current = false;
    }
  }, [open, focusedDate]);

  function setValue(value: string) {
    setText(value);
    inputRef.current?.setCustomValidity(
      value && !parseDisplayDate(value) ? "Enter a valid date in DD.MM.YYYY format." : "",
    );
  }

  function close(restoreFocus: boolean) {
    setOpen(false);
    if (restoreFocus) triggerRef.current?.focus();
  }

  function select(date: Date) {
    const formatted = toBackendDate(isoDate(date));
    if (!formatted) return;
    setValue(formatted);
    close(true);
  }

  function moveFocus(date: Date) {
    if (!toBackendDate(isoDate(date))) return;
    focusDay.current = true;
    setFocusedDate(date);
  }

  function onDayKeyDown(event: KeyboardEvent<HTMLButtonElement>) {
    const date = new Date(focusedDate);
    switch (event.key) {
      case "ArrowLeft": date.setDate(date.getDate() - 1); break;
      case "ArrowRight": date.setDate(date.getDate() + 1); break;
      case "ArrowUp": date.setDate(date.getDate() - 7); break;
      case "ArrowDown": date.setDate(date.getDate() + 7); break;
      case "Home": date.setDate(date.getDate() - date.getDay()); break;
      case "End": date.setDate(date.getDate() + 6 - date.getDay()); break;
      case "PageUp": moveFocus(moveMonths(date, event.shiftKey ? -12 : -1)); event.preventDefault(); return;
      case "PageDown": moveFocus(moveMonths(date, event.shiftKey ? 12 : 1)); event.preventDefault(); return;
      default: return;
    }
    event.preventDefault();
    moveFocus(date);
  }

  const firstDay = new Date(focusedDate.getFullYear(), focusedDate.getMonth(), 1);
  const daysInMonth = new Date(focusedDate.getFullYear(), focusedDate.getMonth() + 1, 0).getDate();
  const cells = Array.from({ length: Math.ceil((firstDay.getDay() + daysInMonth) / 7) * 7 }, (_, index) => {
    const day = index - firstDay.getDay() + 1;
    return day > 0 && day <= daysInMonth
      ? new Date(focusedDate.getFullYear(), focusedDate.getMonth(), day)
      : null;
  });

  return (
    <div
      className="filter-field date-filter"
      lang="en-US"
      onBlur={(event) => {
        if (!event.currentTarget.contains(event.relatedTarget)) setOpen(false);
      }}
      onKeyDown={(event) => {
        if (open && event.key === "Escape") {
          event.preventDefault();
          close(true);
        }
      }}
    >
      <label htmlFor={id}>{label}</label>
      <div className="date-filter-control">
        <input
          ref={inputRef}
          id={id}
          type="text"
          placeholder="DD.MM.YYYY"
          aria-describedby={`${id}-hint`}
          value={text}
          onChange={(event) => setValue(event.target.value)}
        />
        <input type="hidden" name={name} value={selected ?? ""} />
        <button
          ref={triggerRef}
          type="button"
          className="date-filter-trigger"
          aria-label={`Choose ${label.toLowerCase()} date`}
          aria-haspopup="dialog"
          aria-expanded={open}
          onClick={() => {
            if (open) {
              close(false);
            } else {
              focusDay.current = true;
              setFocusedDate(selected ? dateFromIso(selected) : new Date());
              setOpen(true);
            }
          }}
        >
          <svg aria-hidden="true" viewBox="0 0 24 24" fill="none">
            <rect x="3" y="5" width="18" height="16" rx="2" />
            <path d="M7 3v4M17 3v4M3 10h18" />
          </svg>
        </button>
      </div>
      <span className="sr-only" id={`${id}-hint`}>Enter a date in DD.MM.YYYY format.</span>
      {open && (
        <div className="date-calendar" role="dialog" aria-label={`Choose ${label.toLowerCase()} date`}>
          <div className="date-calendar-heading">
            <button type="button" aria-label="Previous month" onClick={() => setFocusedDate(moveMonths(focusedDate, -1))}>‹</button>
            <strong aria-live="polite">{monthFormat.format(focusedDate)}</strong>
            <button type="button" aria-label="Next month" onClick={() => setFocusedDate(moveMonths(focusedDate, 1))}>›</button>
          </div>
          <table role="grid" aria-label={monthFormat.format(focusedDate)}>
            <thead>
              <tr>{weekdays.map((day) => <th key={day} scope="col" abbr={day}>{day.slice(0, 2)}</th>)}</tr>
            </thead>
            <tbody>
              {Array.from({ length: cells.length / 7 }, (_, week) => (
                <tr key={week}>
                  {cells.slice(week * 7, week * 7 + 7).map((date, index) => {
                    const value = date ? isoDate(date) : "";
                    const focused = value === isoDate(focusedDate);
                    return (
                      <td key={index} aria-selected={date ? value === selected : undefined}>
                        {date && (
                          <button
                            ref={focused ? dayRef : undefined}
                            type="button"
                            tabIndex={focused ? 0 : -1}
                            aria-label={dayFormat.format(date)}
                            aria-current={value === isoDate(new Date()) ? "date" : undefined}
                            onClick={() => select(date)}
                            onKeyDown={onDayKeyDown}
                          >
                            {date.getDate()}
                          </button>
                        )}
                      </td>
                    );
                  })}
                </tr>
              ))}
            </tbody>
          </table>
          <div className="date-calendar-actions">
            <button type="button" onClick={() => { setValue(""); close(true); }}>Clear</button>
            <button type="button" onClick={() => select(new Date())}>Today</button>
          </div>
        </div>
      )}
    </div>
  );
}
