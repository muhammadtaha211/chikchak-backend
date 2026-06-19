import { useState } from 'react';
import Paper from '@mui/material/Paper';
import Typography from '@mui/material/Typography';
import Table from '@mui/material/Table';
import TableContainer from '@mui/material/TableContainer';
import TableHead from '@mui/material/TableHead';
import TableBody from '@mui/material/TableBody';
import TableRow from '@mui/material/TableRow';
import TableCell from '@mui/material/TableCell';
import Button from '@mui/material/Button';
import Stack from '@mui/material/Stack';
import TextField from '@mui/material/TextField';
import MenuItem from '@mui/material/MenuItem';
import Chip from '@mui/material/Chip';
import { api } from '../api/client';
import { useList } from '../useList';
import { useToast } from '../toast';
import Pager from '../components/Pager';
import ConfirmDialog from '../components/ConfirmDialog';
import TableStateRow from '../components/TableStateRow';
import { fmtDateTime, REPORT_STATUSES, reportStatusColor } from '../format';

// Dialog copy per action type.
const ACTION_CONFIG = {
  remove: {
    title: 'Remove reported content?',
    message:
      'This blanks the reported chat message and clears the reported rating comment, then marks the report actioned.',
    confirmLabel: 'Remove content',
    confirmColor: 'error',
  },
  dismiss: {
    title: 'Dismiss report?',
    message: 'Mark this report reviewed with no action taken.',
    confirmLabel: 'Dismiss',
    confirmColor: 'primary',
  },
  suspend: {
    title: 'Suspend reported user?',
    message: 'The reported user will no longer be able to log in.',
    confirmLabel: 'Suspend user',
    confirmColor: 'error',
  },
};

function contentLabel(report) {
  const parts = [];
  if (report.chatMessageId) parts.push(`Msg #${report.chatMessageId}`);
  if (report.ratingId) parts.push(`Rating #${report.ratingId}`);
  return parts.length ? parts.join(', ') : '—';
}

export default function ReportsPage() {
  const [page, setPage] = useState(0);
  const [size, setSize] = useState(20);
  const [status, setStatus] = useState('OPEN');
  const [action, setAction] = useState(null); // { type, report }
  const [busy, setBusy] = useState(false);
  const showToast = useToast();

  const { items, loading, error, reload } = useList(
    () => api('/api/admin/reports', { params: { status, page, size } }),
    [status, page, size]
  );

  async function runAction(reason) {
    setBusy(true);
    try {
      const { type, report } = action;
      if (type === 'remove') {
        await api(`/api/admin/reports/${report.id}/remove-content`, { method: 'POST', body: { reason } });
        showToast('Reported content removed');
      } else if (type === 'dismiss') {
        await api(`/api/admin/reports/${report.id}/dismiss`, { method: 'POST', body: { reason } });
        showToast('Report dismissed');
      } else if (type === 'suspend') {
        await api(`/api/admin/users/${report.reportedUserId}/suspend`, { method: 'POST', body: { reason } });
        showToast(`${report.reportedUserName || 'User'} suspended`);
      }
      setAction(null);
      reload();
    } catch (e) {
      showToast(e.message, 'error');
    } finally {
      setBusy(false);
    }
  }

  const dialog = action ? ACTION_CONFIG[action.type] : null;

  return (
    <>
      <Typography variant="h5" sx={{ mb: 2 }}>
        Content Reports
      </Typography>
      <Paper>
        <Stack direction="row" spacing={2} sx={{ p: 2 }}>
          <TextField
            select
            size="small"
            label="Status"
            value={status}
            onChange={(e) => {
              setStatus(e.target.value);
              setPage(0);
            }}
            sx={{ width: 200 }}
          >
            <MenuItem value="">All</MenuItem>
            {REPORT_STATUSES.map((s) => (
              <MenuItem key={s} value={s}>
                {s}
              </MenuItem>
            ))}
          </TextField>
        </Stack>

        <TableContainer>
          <Table size="small">
            <TableHead>
              <TableRow>
                <TableCell>ID</TableCell>
                <TableCell>Reporter</TableCell>
                <TableCell>Reported user</TableCell>
                <TableCell>Reason</TableCell>
                <TableCell>Content</TableCell>
                <TableCell>Status</TableCell>
                <TableCell>Created</TableCell>
                <TableCell align="right">Actions</TableCell>
              </TableRow>
            </TableHead>
            <TableBody>
              {loading || error || items.length === 0 ? (
                <TableStateRow colSpan={8} loading={loading} error={error} emptyMessage="No reports found" />
              ) : (
                items.map((r) => (
                  <TableRow key={r.id} hover>
                    <TableCell>{r.id}</TableCell>
                    <TableCell>{r.reporterName}</TableCell>
                    <TableCell>{r.reportedUserName}</TableCell>
                    <TableCell sx={{ maxWidth: 280, whiteSpace: 'pre-wrap' }}>{r.reason || '—'}</TableCell>
                    <TableCell>{contentLabel(r)}</TableCell>
                    <TableCell>
                      <Chip size="small" label={r.status} color={reportStatusColor(r.status)} />
                    </TableCell>
                    <TableCell>{fmtDateTime(r.createdAt)}</TableCell>
                    <TableCell align="right">
                      {r.status === 'OPEN' ? (
                        <Stack direction="row" spacing={1} justifyContent="flex-end">
                          <Button
                            size="small"
                            variant="outlined"
                            color="error"
                            onClick={() => setAction({ type: 'remove', report: r })}
                          >
                            Remove content
                          </Button>
                          <Button
                            size="small"
                            variant="outlined"
                            color="error"
                            onClick={() => setAction({ type: 'suspend', report: r })}
                          >
                            Suspend user
                          </Button>
                          <Button size="small" onClick={() => setAction({ type: 'dismiss', report: r })}>
                            Dismiss
                          </Button>
                        </Stack>
                      ) : (
                        <Typography variant="caption" color="text.secondary">
                          {r.resolvedBy ? `by ${r.resolvedBy}` : ''}
                          {r.resolutionNote ? ` — ${r.resolutionNote}` : ''}
                        </Typography>
                      )}
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </TableContainer>
        <Pager
          page={page}
          size={size}
          count={items.length}
          onPage={setPage}
          onSize={(s) => {
            setSize(s);
            setPage(0);
          }}
        />
      </Paper>

      <ConfirmDialog
        open={!!action}
        title={dialog?.title}
        message={dialog?.message}
        confirmLabel={dialog?.confirmLabel}
        confirmColor={dialog?.confirmColor}
        withReason
        busy={busy}
        onConfirm={runAction}
        onClose={() => setAction(null)}
      />
    </>
  );
}
