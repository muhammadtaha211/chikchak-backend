import { useEffect, useState } from 'react';
import Dialog from '@mui/material/Dialog';
import DialogTitle from '@mui/material/DialogTitle';
import DialogContent from '@mui/material/DialogContent';
import DialogContentText from '@mui/material/DialogContentText';
import DialogActions from '@mui/material/DialogActions';
import Button from '@mui/material/Button';
import TextField from '@mui/material/TextField';
import CircularProgress from '@mui/material/CircularProgress';

// "Are you sure?" dialog for destructive admin actions, with an optional
// free-text reason field (sent to the backend's audit log).
export default function ConfirmDialog({
  open,
  title,
  message,
  confirmLabel = 'Confirm',
  confirmColor = 'error',
  withReason = false,
  busy = false,
  onConfirm,
  onClose,
}) {
  const [reason, setReason] = useState('');

  // Clear the reason each time the dialog is reopened for a new target.
  useEffect(() => {
    if (open) setReason('');
  }, [open]);

  return (
    <Dialog open={open} onClose={busy ? undefined : onClose} maxWidth="xs" fullWidth>
      <DialogTitle>{title}</DialogTitle>
      <DialogContent>
        <DialogContentText>{message}</DialogContentText>
        {withReason && (
          <TextField
            autoFocus
            fullWidth
            multiline
            minRows={2}
            label="Reason (optional)"
            value={reason}
            onChange={(e) => setReason(e.target.value)}
            sx={{ mt: 2 }}
          />
        )}
      </DialogContent>
      <DialogActions>
        <Button onClick={onClose} disabled={busy}>
          Cancel
        </Button>
        <Button
          variant="contained"
          color={confirmColor}
          disabled={busy}
          startIcon={busy ? <CircularProgress size={16} color="inherit" /> : null}
          onClick={() => onConfirm(reason.trim() || undefined)}
        >
          {confirmLabel}
        </Button>
      </DialogActions>
    </Dialog>
  );
}
