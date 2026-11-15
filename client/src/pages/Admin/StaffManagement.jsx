import React from "react";
import {
  Loader2,
  Mail,
  RefreshCw,
  ShieldCheck,
  UserCog,
  UserPlus,
} from "lucide-react";
import toast from "react-hot-toast";

import Title from "../../components/Admin/Title";
import {
  createStaffAccount,
  getStaffAccounts,
} from "../../lib/tickets";

const EMPTY_FORM = {
  name: "",
  email: "",
  password: "",
};

export default function StaffManagement() {
  const [staff, setStaff] = React.useState([]);
  const [loading, setLoading] = React.useState(true);
  const [saving, setSaving] = React.useState(false);
  const [error, setError] = React.useState("");
  const [form, setForm] = React.useState(EMPTY_FORM);

  const loadStaff = React.useCallback(async () => {
    setLoading(true);
    setError("");

    try {
      const data = await getStaffAccounts();
      setStaff(Array.isArray(data) ? data : []);
    } catch (requestError) {
      setError(requestError.message || "Unable to load staff accounts");
    } finally {
      setLoading(false);
    }
  }, []);

  React.useEffect(() => {
    loadStaff();
  }, [loadStaff]);

  const update = (field) => (event) =>
    setForm((current) => ({
      ...current,
      [field]: event.target.value,
    }));

  const submit = async (event) => {
    event.preventDefault();

    if (saving) return;

    setSaving(true);

    try {
      await createStaffAccount({
        name: form.name.trim(),
        email: form.email.trim(),
        password: form.password || null,
      });

      toast.success("Staff access saved");
      setForm(EMPTY_FORM);
      await loadStaff();
    } catch (requestError) {
      toast.error(requestError.message || "Unable to save staff access");
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="mx-auto max-w-[1100px]">
      <Title
        title="Staff access"
        subtitle="Create or promote accounts that can scan and admit tickets."
      />

      <div className="mt-6 grid gap-6 lg:grid-cols-[380px_1fr]">
        <section className="h-fit rounded-2xl border border-white/10 bg-[#0b1017] p-5 sm:p-6">
          <div className="flex items-center gap-3">
            <span className="grid h-10 w-10 place-items-center rounded-xl bg-cyan-300/10 text-cyan-300">
              <UserPlus size={19} />
            </span>

            <div>
              <h2 className="text-sm font-semibold">Add staff access</h2>
              <p className="text-xs text-white/35">
                Existing customers can be promoted.
              </p>
            </div>
          </div>

          <form onSubmit={submit} className="mt-6 space-y-4">
            <Field label="Full name">
              <input
                value={form.name}
                onChange={update("name")}
                required
                maxLength={120}
                placeholder="Theatre staff name"
                className="w-full rounded-xl border border-white/10 bg-white/[.04] px-3.5 py-3 text-sm text-white outline-none placeholder:text-white/25 focus:border-cyan-300/40"
              />
            </Field>

            <Field label="Email">
              <input
                type="email"
                value={form.email}
                onChange={update("email")}
                required
                maxLength={190}
                placeholder="staff@cinema.com"
                className="w-full rounded-xl border border-white/10 bg-white/[.04] px-3.5 py-3 text-sm text-white outline-none placeholder:text-white/25 focus:border-cyan-300/40"
              />
            </Field>

            <Field label="Password">
              <input
                type="password"
                value={form.password}
                onChange={update("password")}
                minLength={8}
                maxLength={120}
                autoComplete="new-password"
                placeholder="At least 8 characters"
                className="w-full rounded-xl border border-white/10 bg-white/[.04] px-3.5 py-3 text-sm text-white outline-none placeholder:text-white/25 focus:border-cyan-300/40"
              />
            </Field>

            <p className="text-[11px] leading-5 text-white/30">
              For an existing customer, the password can be left blank to keep
              their current password. Passwords are hashed and never returned
              by the API.
            </p>

            <button
              type="submit"
              disabled={saving}
              className="flex w-full items-center justify-center gap-2 rounded-xl bg-cyan-300 py-3 text-sm font-semibold text-slate-950 transition hover:bg-cyan-200 disabled:cursor-not-allowed disabled:opacity-45"
            >
              {saving ? (
                <Loader2 className="animate-spin" size={16} />
              ) : (
                <ShieldCheck size={16} />
              )}
              {saving ? "Saving..." : "Save staff access"}
            </button>
          </form>
        </section>

        <section className="rounded-2xl border border-white/10 bg-[#0b1017] p-5 sm:p-6">
          <div className="flex items-center justify-between gap-4">
            <div>
              <h2 className="text-sm font-semibold">Authorised staff</h2>
              <p className="mt-1 text-xs text-white/35">
                These accounts can open /staff/scanner.
              </p>
            </div>

            <button
              type="button"
              onClick={loadStaff}
              disabled={loading}
              className="flex items-center gap-2 rounded-full border border-white/10 px-3 py-2 text-xs text-white/55 transition hover:border-cyan-300/30 hover:text-white disabled:opacity-40"
            >
              <RefreshCw size={14} className={loading ? "animate-spin" : ""} />
              Refresh
            </button>
          </div>

          {error && (
            <p className="mt-5 rounded-xl border border-red-400/20 bg-red-400/[.06] p-3 text-xs text-red-200">
              {error}
            </p>
          )}

          {loading ? (
            <div className="mt-10 flex items-center justify-center gap-2 py-10 text-sm text-white/35">
              <Loader2 className="animate-spin" size={18} />
              Loading staff...
            </div>
          ) : staff.length ? (
            <div className="mt-5 divide-y divide-white/[.07]">
              {staff.map((member) => (
                <div
                  key={member.id}
                  className="flex items-center justify-between gap-4 py-4"
                >
                  <div className="flex min-w-0 items-center gap-3">
                    <span className="grid h-10 w-10 shrink-0 place-items-center rounded-xl bg-white/[.05] text-cyan-300">
                      <UserCog size={18} />
                    </span>

                    <div className="min-w-0">
                      <p className="truncate text-sm font-medium">
                        {member.name}
                      </p>

                      <p className="mt-0.5 flex items-center gap-1.5 truncate text-xs text-white/35">
                        <Mail size={12} />
                        {member.email}
                      </p>
                    </div>
                  </div>

                  <span className="shrink-0 rounded-full border border-cyan-300/20 bg-cyan-300/10 px-2.5 py-1 text-[10px] font-semibold uppercase tracking-wider text-cyan-200">
                    {member.role}
                  </span>
                </div>
              ))}
            </div>
          ) : (
            <div className="mt-10 rounded-xl border border-dashed border-white/10 p-8 text-center">
              <UserCog size={30} className="mx-auto text-white/20" />

              <p className="mt-3 text-sm text-white/50">
                No staff accounts yet
              </p>

              <p className="mt-1 text-xs text-white/30">
                Add an account using the form to enable scanner access.
              </p>
            </div>
          )}
        </section>
      </div>
    </div>
  );
}

function Field({ label, children }) {
  return (
    <label className="block">
      <span className="mb-2 block text-[10px] uppercase tracking-[.18em] text-white/35">
        {label}
      </span>

      {children}
    </label>
  );
}
