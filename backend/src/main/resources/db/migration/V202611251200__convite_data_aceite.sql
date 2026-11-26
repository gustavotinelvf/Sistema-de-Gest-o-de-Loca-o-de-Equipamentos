-- Data em que o convite foi aceito (preenchida por Convite.aceitar).
ALTER TABLE convite ADD COLUMN IF NOT EXISTS data_aceite TIMESTAMPTZ;