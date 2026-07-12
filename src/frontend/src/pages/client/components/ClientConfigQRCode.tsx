import { useEffect, useState } from "react";
import { Box } from "@cloudscape-design/components";
import { useFlashbarContext } from "../../../context/FlashbarContextProvider";
import { networkClientClient } from "../../../api/HTTPClients";
import LoadingState from "../../../components/state/LoadingState";

interface ClientConfigQRCodeProps {
  networkName: string;
  clientName: string;
  blur?: boolean;
}
const ClientConfigQRCode = (props: ClientConfigQRCodeProps): JSX.Element => {
  const { networkName, clientName, blur = false } = props;

  const [qrImageUrl, setQrImageUrl] = useState<string | null>(null);
  const [loading, setLoading] = useState(true);

  const { addFlashbarItem } = useFlashbarContext();

  useEffect(() => {
    (async function () {
      try {
        const response = await networkClientClient.getNetworkClientConfigFileQR({
          networkName,
          clientName,
        });
        // Convert blob to object URL for img src
        const blob = new Blob([response], { type: "image/png" });
        const url = URL.createObjectURL(blob);
        setQrImageUrl(url);
      } catch (error) {
        const errorMessage =
          error.response?.data?.message || error.response?.data?.error || error.message;
        console.error(errorMessage);
        addFlashbarItem({
          type: "error",
          header: "Failure in GetNetworkClientConfigFileQR",
          content: errorMessage,
          dismissLabel: "Dismiss",
          duration: 10000,
        });
      } finally {
        setLoading(false);
      }
    })();

    // Cleanup: revoke object URL when component unmounts
    return () => {
      if (qrImageUrl) {
        URL.revokeObjectURL(qrImageUrl);
      }
    };
  }, [networkName, clientName]);

  return (
    <Box textAlign="center">
      <LoadingState loading={loading} height={512} width={512}>
        {qrImageUrl ? (
          <img
            src={qrImageUrl}
            alt={`QR code for ${clientName} configuration`}
            style={{
              maxWidth: "60%",
              height: "auto",
              filter: blur ? "blur(10px)" : "none",
              transition: "filter 0.3s ease",
            }}
          />
        ) : (
          <Box variant="p" color="text-status-error">
            Failed to load QR code
          </Box>
        )}
      </LoadingState>
    </Box>
  );
};
export default ClientConfigQRCode;
